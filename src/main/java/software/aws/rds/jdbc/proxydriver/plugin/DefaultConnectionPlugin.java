/*
 * AWS JDBC Proxy Driver
 * Copyright Amazon.com Inc. or affiliates.
 * See the LICENSE file in the project root for more information.
 */

package software.aws.rds.jdbc.proxydriver.plugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import software.aws.rds.jdbc.proxydriver.ConnectionPlugin;
import software.aws.rds.jdbc.proxydriver.ConnectionProvider;
import software.aws.rds.jdbc.proxydriver.HostListProviderService;
import software.aws.rds.jdbc.proxydriver.HostSpec;
import software.aws.rds.jdbc.proxydriver.JdbcCallable;
import software.aws.rds.jdbc.proxydriver.NodeChangeOptions;
import software.aws.rds.jdbc.proxydriver.OldConnectionSuggestedAction;
import software.aws.rds.jdbc.proxydriver.PluginService;

/**
 * This connection plugin will always be the last plugin in the connection plugin chain, and will
 * invoke the JDBC method passed down the chain.
 */
public final class DefaultConnectionPlugin implements ConnectionPlugin {

  private static final transient Logger LOGGER =
      Logger.getLogger(DefaultConnectionPlugin.class.getName());
  private static final Set<String> subscribedMethods =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList("*", "openInitialConnection")));

  private final ConnectionProvider connectionProvider;
  private final PluginService pluginService;

  public DefaultConnectionPlugin(
      PluginService pluginService, ConnectionProvider connectionProvider) {
    if (pluginService == null) {
      throw new IllegalArgumentException("currentConnectionProvider");
    }
    if (connectionProvider == null) {
      throw new IllegalArgumentException("connectionProvider");
    }

    this.pluginService = pluginService;
    this.connectionProvider = connectionProvider;
  }

  @Override
  public Set<String> getSubscribedMethods() {
    return subscribedMethods;
  }

  @Override
  public <T, E extends Exception> T execute(
      Class<T> resultClass,
      Class<E> exceptionClass,
      Object methodInvokeOn,
      String methodName,
      JdbcCallable<T, E> jdbcMethodFunc,
      Object[] jdbcMethodArgs)
      throws E {

    LOGGER.log(Level.FINEST, String.format("Executing method %s", methodName));
    return jdbcMethodFunc.call();
  }

  @Override
  public Connection connect(
      String driverProtocol,
      HostSpec hostSpec,
      Properties props,
      boolean isInitialConnection,
      JdbcCallable<Connection, SQLException> connectFunc)
      throws SQLException {

    Connection conn = this.connectionProvider.connect(driverProtocol, hostSpec, props);

    // It's guaranteed that this plugin is always the last in plugin chain so connectFunc can be
    // omitted.

    return conn;
  }

  @Override
  public void initHostProvider(
      final String driverProtocol,
      final String initialUrl,
      final Properties props,
      final HostListProviderService hostListProviderService,
      final JdbcCallable<Void, SQLException> initHostProviderFunc)
      throws SQLException {

    // do nothing
    // It's guaranteed that this plugin is always the last in plugin chain so initHostProviderFunc
    // can be omitted.
  }

  @Override
  public OldConnectionSuggestedAction notifyConnectionChanged(EnumSet<NodeChangeOptions> changes)
      throws SQLException {
    return OldConnectionSuggestedAction.NO_OPINION;
  }

  @Override
  public void notifyNodeListChanged(Map<String, EnumSet<NodeChangeOptions>> changes)
      throws SQLException {
    // do nothing
  }

  @Override
  public void releaseResources() {
    // do nothing
  }
}
