/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package software.amazon.jdbc.plugin;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import software.amazon.jdbc.HostSpec;
import software.amazon.jdbc.JdbcCallable;
import software.amazon.jdbc.NodeChangeOptions;
import software.amazon.jdbc.PluginService;
import software.amazon.jdbc.dialect.TopologyAwareDatabaseCluster;
import software.amazon.jdbc.hostlistprovider.AuroraHostListProvider;
import software.amazon.jdbc.plugin.failover.FailoverSQLException;
import software.amazon.jdbc.util.RdsUtils;
import software.amazon.jdbc.util.StringUtils;
import software.amazon.jdbc.util.SubscribedMethodHelper;

public class AuroraConnectionTrackerPlugin extends AbstractConnectionPlugin {

  private static final Logger LOGGER = Logger.getLogger(AuroraConnectionTrackerPlugin.class.getName());

  static final String METHOD_ABORT = "Connection.abort";
  static final String METHOD_CLOSE = "Connection.close";
  private static final Set<String> subscribedMethods =
      Collections.unmodifiableSet(new HashSet<String>() {
        {
          addAll(SubscribedMethodHelper.NETWORK_BOUND_METHODS);
          add("connect");
          add("forceConnect");
          add("notifyNodeListChanged");
        }
      });

  private final PluginService pluginService;
  private final Properties props;
  private final RdsUtils rdsHelper;
  private String clusterInstanceTemplate;
  private final OpenedConnectionTracker tracker;

  AuroraConnectionTrackerPlugin(final PluginService pluginService, final Properties props) {
    this(pluginService, props, new RdsUtils(), new OpenedConnectionTracker());
  }

  AuroraConnectionTrackerPlugin(
      final PluginService pluginService,
      final Properties props,
      final RdsUtils rdsUtils,
      final OpenedConnectionTracker tracker) {
    this.pluginService = pluginService;
    this.props = props;
    this.rdsHelper = rdsUtils;
    this.tracker = tracker;
  }

  @Override
  public Set<String> getSubscribedMethods() {
    return subscribedMethods;
  }

  @Override
  public Connection connect(final String driverProtocol, final HostSpec hostSpec, final Properties props,
      final boolean isInitialConnection, final JdbcCallable<Connection, SQLException> connectFunc) throws SQLException {
    return connectInternal(hostSpec, connectFunc);
  }

  public Connection connectInternal(
      final HostSpec hostSpec, final JdbcCallable<Connection, SQLException> connectFunc)
      throws SQLException {

    final Connection conn = connectFunc.call();
    final HostSpec currentHostSpec = (this.pluginService.getCurrentHostSpec() != null)
        ? this.pluginService.getCurrentHostSpec()
        : hostSpec;

    if (conn != null) {
      if (!rdsHelper.isRdsInstance(currentHostSpec.getHost())) {
        // Remove pre-existing instance endpoint.
        currentHostSpec.getAliases().stream()
            .filter(this.rdsHelper::isRdsInstance)
            .findAny()
            .ifPresent(currentHostSpec::removeAlias);
        currentHostSpec.addAlias(getInstanceEndpoint(conn, currentHostSpec));
      }
    }

    tracker.populateOpenedConnectionQueue(currentHostSpec, conn);
    tracker.logOpenedConnections();

    return conn;
  }

  @Override
  public Connection forceConnect(String driverProtocol, HostSpec hostSpec, Properties props,
      boolean isInitialConnection, JdbcCallable<Connection, SQLException> forceConnectFunc) throws SQLException {
    return connectInternal(hostSpec, forceConnectFunc);
  }

  private String getInstanceEndpointPattern(final String url) {
    if (StringUtils.isNullOrEmpty(this.clusterInstanceTemplate)) {
      this.clusterInstanceTemplate = AuroraHostListProvider.CLUSTER_INSTANCE_HOST_PATTERN.getString(this.props) == null
          ? rdsHelper.getRdsInstanceHostPattern(url)
          : AuroraHostListProvider.CLUSTER_INSTANCE_HOST_PATTERN.getString(this.props);
    }

    return this.clusterInstanceTemplate;
  }

  @Override
  public <T, E extends Exception> T execute(final Class<T> resultClass, final Class<E> exceptionClass,
      final Object methodInvokeOn, final String methodName, final JdbcCallable<T, E> jdbcMethodFunc,
      final Object[] jdbcMethodArgs) throws E {
    String originalHost = this.pluginService.getCurrentHostSpec().getUrl();
    if (!this.rdsHelper.isRdsInstance(originalHost)) {
      originalHost = this.pluginService.getCurrentHostSpec().getAliases().stream()
          .filter(this.rdsHelper::isRdsInstance).findAny()
          .orElse(null);
    }

    try {
      final T result = jdbcMethodFunc.call();
      if ((methodName.equals(METHOD_CLOSE) || methodName.equals(METHOD_ABORT))) {
        tracker.invalidateCurrentConnection(originalHost, this.pluginService.getCurrentConnection());
      }
      return result;
    } catch (final Exception e) {
      if (e instanceof FailoverSQLException) {
        tracker.invalidateAllConnections(originalHost);
        tracker.logOpenedConnections();
      }
      throw e;
    }
  }

  @Override
  public void notifyNodeListChanged(final Map<String, EnumSet<NodeChangeOptions>> changes) {
    final StringBuilder sb = new StringBuilder("Tracker Changes:");
    for (final Map.Entry<String, EnumSet<NodeChangeOptions>> change : changes.entrySet()) {
      if (sb.length() > 0) {
        sb.append("\n");
      }
      sb.append(String.format("\tHost '%s': %s", change.getKey(), change.getValue()));
    }
    LOGGER.finest(sb.toString());

    for (final String node : changes.keySet()) {
      if (isRoleChanged(changes.get(node))) {
        tracker.invalidateAllConnections(node);
      }
    }
  }

  private boolean isRoleChanged(final EnumSet<NodeChangeOptions> changes) {
    return changes.contains(NodeChangeOptions.PROMOTED_TO_READER);
  }

  public String getInstanceEndpoint(final Connection conn, final HostSpec host) {
    String instanceName = "?";

    if (!(this.pluginService.getDialect() instanceof TopologyAwareDatabaseCluster)) {
      return instanceName;
    }
    final TopologyAwareDatabaseCluster topologyAwareDialect =
        (TopologyAwareDatabaseCluster) this.pluginService.getDialect();

    try (final Statement stmt = conn.createStatement();
        final ResultSet resultSet = stmt.executeQuery(topologyAwareDialect.getNodeIdQuery())) {
      if (resultSet.next()) {
        instanceName = resultSet.getString(1);
      }
      String instanceEndpoint = getInstanceEndpointPattern(host.getHost());
      instanceEndpoint = host.isPortSpecified() ? instanceEndpoint + ":" + host.getPort() : instanceEndpoint;
      return instanceEndpoint.replace("?", instanceName);
    } catch (final SQLException e) {
      return instanceName;
    }
  }
}
