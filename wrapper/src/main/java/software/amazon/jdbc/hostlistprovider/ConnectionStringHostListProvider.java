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

package software.amazon.jdbc.hostlistprovider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.checkerframework.checker.nullness.qual.NonNull;
import software.amazon.jdbc.AwsWrapperProperty;
import software.amazon.jdbc.HostListProviderService;
import software.amazon.jdbc.HostRole;
import software.amazon.jdbc.HostSpec;
import software.amazon.jdbc.PropertyDefinition;
import software.amazon.jdbc.util.ConnectionUrlParser;
import software.amazon.jdbc.util.Messages;
import software.amazon.jdbc.util.RdsUtils;
import software.amazon.jdbc.util.StringUtils;

public class ConnectionStringHostListProvider implements StaticHostListProvider {

  public static final AwsWrapperProperty SINGLE_WRITER_CONNECTION_STRING =
      new AwsWrapperProperty(
          "singleWriterConnectionString",
          "false",
          "Set to true if you are providing a connection string with multiple comma-delimited hosts and your "
              + "cluster has only one writer. The writer must be the first host in the connection string");

  public static final AwsWrapperProperty CLUSTER_ID = new AwsWrapperProperty(
      "clusterId", "",
      "A unique identifier for the cluster. "
          + "Connections with the same cluster id share a cluster topology cache. "
          + "If unspecified, a cluster id is automatically created for AWS RDS clusters.");

  final List<HostSpec> hostList = new ArrayList<>();
  Properties properties;
  private boolean isInitialized = false;
  private final boolean isSingleWriterConnectionString;
  private final ConnectionUrlParser connectionUrlParser;
  private final String initialUrl;
  private final HostListProviderService hostListProviderService;
  private final String clusterId;

  private final RdsUtils rdsUtils = new RdsUtils();

  static {
    PropertyDefinition.registerPluginProperties(ConnectionStringHostListProvider.class);
  }

  public ConnectionStringHostListProvider(
      final @NonNull Properties properties,
      final String initialUrl,
      final @NonNull HostListProviderService hostListProviderService) {
    this(properties, initialUrl, hostListProviderService, new ConnectionUrlParser());
  }

  ConnectionStringHostListProvider(
      final @NonNull Properties properties,
      final String initialUrl,
      final @NonNull HostListProviderService hostListProviderService,
      final @NonNull ConnectionUrlParser connectionUrlParser) {

    this.isSingleWriterConnectionString = SINGLE_WRITER_CONNECTION_STRING.getBoolean(properties);
    this.initialUrl = initialUrl;
    this.connectionUrlParser = connectionUrlParser;
    this.hostListProviderService = hostListProviderService;
    this.clusterId = CLUSTER_ID.getString(properties);
  }

  private void init() throws SQLException {
    if (this.isInitialized) {
      return;
    }
    this.hostList.addAll(
        this.connectionUrlParser.getHostsFromConnectionUrl(this.initialUrl, this.isSingleWriterConnectionString,
            () -> this.hostListProviderService.getHostSpecBuilder()));
    if (this.hostList.isEmpty()) {
      throw new SQLException(Messages.get("ConnectionStringHostListProvider.parsedListEmpty",
          new Object[] {this.initialUrl}));
    }
    this.hostListProviderService.setInitialConnectionHostSpec(this.hostList.get(0));
    this.isInitialized = true;
  }

  @Override
  public List<HostSpec> refresh() throws SQLException {
    init();
    return Collections.unmodifiableList(hostList);
  }

  @Override
  public List<HostSpec> refresh(final Connection connection) throws SQLException {
    init();
    return this.refresh();
  }

  @Override
  public List<HostSpec> forceRefresh() throws SQLException {
    init();
    return Collections.unmodifiableList(hostList);
  }

  @Override
  public List<HostSpec> forceRefresh(final Connection connection) throws SQLException {
    init();
    return this.forceRefresh();
  }

  @Override
  public HostRole getHostRole(Connection connection) {
    throw new UnsupportedOperationException("ConnectionStringHostListProvider does not support getHostRole");
  }

  @Override
  public HostSpec identifyConnection(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(
        Messages.get("ConnectionStringHostListProvider.unsupportedIdentifyConnection"));
  }

  @Override
  public String getClusterId() {
    if (!StringUtils.isNullOrEmpty(this.clusterId)) {
      return this.clusterId;
    }

    final HostSpec hostSpec = this.hostListProviderService.getInitialConnectionHostSpec();
    if (hostSpec == null) {
      return null;
    }

    return this.rdsUtils.removeGreenInstancePrefix(hostSpec.getHost());
  }
}
