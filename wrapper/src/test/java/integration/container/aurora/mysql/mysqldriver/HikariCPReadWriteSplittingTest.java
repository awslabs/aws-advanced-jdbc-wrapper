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

package integration.container.aurora.mysql.mysqldriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.mysql.cj.conf.PropertyKey;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import eu.rekawek.toxiproxy.Proxy;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.jdbc.PropertyDefinition;
import software.amazon.jdbc.ds.AwsWrapperDataSource;
import software.amazon.jdbc.hostlistprovider.AuroraHostListProvider;
import software.amazon.jdbc.plugin.efm.HostMonitoringConnectionPlugin;
import software.amazon.jdbc.plugin.failover.FailoverConnectionPlugin;
import software.amazon.jdbc.plugin.failover.FailoverFailedSQLException;
import software.amazon.jdbc.plugin.failover.FailoverSuccessSQLException;
import software.amazon.jdbc.util.HikariCPSQLException;
import software.amazon.jdbc.util.SqlState;
import software.amazon.jdbc.util.StringUtils;

public class HikariCPReadWriteSplittingTest extends MysqlAuroraMysqlBaseTest {

  private static final Logger logger = Logger.getLogger(
      integration.container.aurora.mysql.mysqldriver.HikariCPReadWriteSplittingTest.class.getName());
  private static HikariDataSource dataSource = null;
  private final List<String> clusterTopology = fetchTopology();

  private List<String> fetchTopology() {
    try {
      final List<String> topology = getTopologyEndpoints();
      // topology should contain a writer and at least one reader
      if (topology == null || topology.size() < 2) {
        fail("Topology does not contain the required instances");
      }
      return topology;
    } catch (final SQLException e) {
      fail("Couldn't fetch cluster topology");
    }

    return null;
  }

  @BeforeAll
  static void setup() {
    System.setProperty("com.zaxxer.hikari.blockUntilFilled", "true");
  }

  @AfterEach
  public void teardown() {
    dataSource.close();
  }

  private static Stream<Arguments> testParameters() {
    return Stream.of(
        Arguments.of(getProps_allPlugins()),
        Arguments.of(getProps_readWritePlugin())
    );
  }

  private static Properties getProps_readWritePlugin() {
    final Properties props = new Properties();
    props.setProperty(PropertyDefinition.PLUGINS.name, "auroraHostList,readWriteSplitting");
    return props;
  }

  private static Properties getProps_allPlugins() {
    final Properties props = new Properties();
    props.setProperty(PropertyDefinition.PLUGINS.name, "readWriteSplitting,failover,efm");
    return props;
  }

  /**
   * After getting successful connections from the pool, the cluster becomes unavailable.
   */
  @ParameterizedTest(name = "test_1_1_hikariCP_lost_connection")
  @MethodSource("testParameters")
  public void test_1_1_hikariCP_lost_connection(final Properties customProps) throws SQLException {
    FailoverConnectionPlugin.FAILOVER_TIMEOUT_MS.set(customProps, "1");
    customProps.setProperty(PropertyKey.socketTimeout.getKeyName(), "500");
    createDataSource(customProps);
    try (final Connection conn = dataSource.getConnection()) {
      assertTrue(conn.isValid(5));

      putDownAllInstances(true);

      final SQLException e = assertThrows(SQLException.class, () -> queryInstanceId(conn));
      if (pluginChainIncludesFailoverPlugin(customProps)) {
        assertTrue(e instanceof FailoverFailedSQLException);
      } else {
        assertEquals(SqlState.COMMUNICATION_ERROR.getState(), e.getSQLState());
      }
      assertFalse(conn.isValid(5));
    }

    assertThrows(SQLTransientConnectionException.class, () -> dataSource.getConnection());
  }

  /**
   * After getting a successful connection from the pool, the connected instance becomes unavailable and the
   * connection fails over to another instance. A connection is then retrieved to check that connections
   * to failed instances are not returned.
   */
  @ParameterizedTest(name = "test_1_2_hikariCP_get_dead_connection")
  @MethodSource("testParameters")
  public void test_1_2_hikariCP_get_dead_connection(final Properties customProps) throws SQLException {
    putDownAllInstances(false);

    final String writer = clusterTopology.get(0);
    final String reader = clusterTopology.get(1);
    final String writerIdentifier = writer.split("\\.")[0];
    final String readerIdentifier = reader.split("\\.")[0];
    logger.fine("Instance to connect to: " + writerIdentifier);
    logger.fine("Instance to fail over to: " + readerIdentifier);

    bringUpInstance(writerIdentifier);
    createDataSource(customProps);

    // Get a valid connection, then make it fail over to a different instance
    try (final Connection conn = dataSource.getConnection()) {
      assertTrue(conn.isValid(5));
      String currentInstance = queryInstanceId(conn);
      assertTrue(currentInstance.equalsIgnoreCase(writerIdentifier));
      bringUpInstance(readerIdentifier);
      putDownInstance(currentInstance);

      final SQLException e = assertThrows(SQLException.class, () -> queryInstanceId(conn));
      if (pluginChainIncludesFailoverPlugin(customProps)) {
        assertTrue(e instanceof FailoverSuccessSQLException);
      } else {
        assertEquals(SqlState.COMMUNICATION_ERROR.getState(), e.getSQLState());
        return;
      }

      // Check the connection is valid after connecting to a different instance
      assertTrue(conn.isValid(5));
      currentInstance = queryInstanceId(conn);
      logger.fine("Connected to instance: " + currentInstance);
      assertTrue(currentInstance.equalsIgnoreCase(readerIdentifier));

      // Try to get a new connection to the failed instance, which times out
      assertThrows(SQLTransientConnectionException.class, () -> dataSource.getConnection());
    }
  }

  /**
   * After getting a successful connection from the pool, the connected instance becomes unavailable and the
   * connection fails over to another instance through the Enhanced Failure Monitor.
   */
  @ParameterizedTest(name = "test_2_1_hikariCP_efm_failover")
  @MethodSource("testParameters")
  public void test_2_1_hikariCP_efm_failover(final Properties customProps) throws SQLException {
    putDownAllInstances(false);

    final String writer = clusterTopology.get(0);
    final String reader = clusterTopology.get(1);
    final String writerIdentifier = writer.split("\\.")[0];
    final String readerIdentifier = reader.split("\\.")[0];
    logger.fine("Instance to connect to: " + writerIdentifier);
    logger.fine("Instance to fail over to: " + readerIdentifier);

    bringUpInstance(writerIdentifier);
    createDataSource(customProps);

    // Get a valid connection, then make it fail over to a different instance
    try (final Connection conn = dataSource.getConnection()) {
      assertTrue(conn.isValid(5));
      String currentInstance = queryInstanceId(conn);
      assertTrue(currentInstance.equalsIgnoreCase(writerIdentifier));
      logger.fine("Connected to instance: " + currentInstance);

      bringUpInstance(readerIdentifier);
      putDownInstance(writerIdentifier);

      final SQLException e = assertThrows(SQLException.class, () -> queryInstanceId(conn));
      if (pluginChainIncludesFailoverPlugin(customProps)) {
        assertTrue(e instanceof FailoverSuccessSQLException);
      } else {
        assertEquals(SqlState.COMMUNICATION_ERROR.getState(), e.getSQLState());
        return;
      }

      // Check the connection is valid after connecting to a different instance
      assertTrue(conn.isValid(5));
      currentInstance = queryInstanceId(conn);
      logger.fine("Connected to instance: " + currentInstance);
      assertTrue(currentInstance.equalsIgnoreCase(readerIdentifier));
    }
  }

  private void putDownInstance(final String targetInstance) {
    final Proxy toPutDown = proxyMap.get(targetInstance);
    disableInstanceConnection(toPutDown);
    logger.fine("Took down " + targetInstance);
  }

  private void putDownAllInstances(final Boolean putDownClusters) {
    logger.fine("Putting down all instances");
    proxyMap.forEach((instance, proxy) -> {
      if (putDownClusters || (proxy != proxyCluster && proxy != proxyReadOnlyCluster)) {
        disableInstanceConnection(proxy);
      }
    });
  }

  private void disableInstanceConnection(final Proxy proxy) {
    try {
      containerHelper.disableConnectivity(proxy);
    } catch (final IOException e) {
      fail("Couldn't disable proxy connectivity");
    }
  }

  private void bringUpInstance(final String targetInstance) {
    final Proxy toBringUp = proxyMap.get(targetInstance);
    containerHelper.enableConnectivity(toBringUp);
    logger.fine("Brought up " + targetInstance);
  }

  private HikariConfig getConfig(Properties customProps) {
    final HikariConfig config = new HikariConfig();
    config.setUsername(AURORA_MYSQL_USERNAME);
    config.setPassword(AURORA_MYSQL_PASSWORD);
    config.setMaximumPoolSize(3);
    config.setReadOnly(true);
    config.setExceptionOverrideClassName(HikariCPSQLException.class.getName());
    config.setInitializationFailTimeout(75000);
    config.setConnectionTimeout(1000);

    config.setDataSourceClassName(AwsWrapperDataSource.class.getName());
    config.addDataSourceProperty("targetDataSourceClassName", "com.mysql.cj.jdbc.MysqlDataSource");
    config.addDataSourceProperty("jdbcProtocol", "jdbc:mysql:");
    config.addDataSourceProperty("portPropertyName", "port");
    config.addDataSourceProperty("serverPropertyName", "serverName");

    final Properties targetDataSourceProps = new Properties();
    targetDataSourceProps.setProperty("serverName", clusterTopology.get(0) + PROXIED_DOMAIN_NAME_SUFFIX);
    targetDataSourceProps.setProperty("port", String.valueOf(MYSQL_PROXY_PORT));
    targetDataSourceProps.setProperty("socketTimeout", "3000");
    targetDataSourceProps.setProperty("connectTimeout", "3000");
    targetDataSourceProps.setProperty("monitoring-connectTimeout", "1000");
    targetDataSourceProps.setProperty("monitoring-socketTimeout", "1000");
    targetDataSourceProps.setProperty(AuroraHostListProvider.CLUSTER_INSTANCE_HOST_PATTERN.name,
        PROXIED_CLUSTER_TEMPLATE);
    targetDataSourceProps.setProperty(HostMonitoringConnectionPlugin.FAILURE_DETECTION_TIME.name, "2000");
    targetDataSourceProps.setProperty(HostMonitoringConnectionPlugin.FAILURE_DETECTION_INTERVAL.name, "1000");
    targetDataSourceProps.setProperty(HostMonitoringConnectionPlugin.FAILURE_DETECTION_COUNT.name, "1");

    final Enumeration<?> propertyNames = customProps.propertyNames();
    while (propertyNames.hasMoreElements()) {
      String propertyName = propertyNames.nextElement().toString();
      if (!StringUtils.isNullOrEmpty(propertyName)) {
        final String propertyValue = customProps.getProperty(propertyName);
        targetDataSourceProps.setProperty(propertyName, propertyValue);
      }
    }

    config.addDataSourceProperty("targetDataSourceProperties", targetDataSourceProps);

    return config;
  }

  private void createDataSource(final Properties customProps) {
    final HikariConfig config = getConfig(customProps);
    dataSource = new HikariDataSource(config);

    final HikariPoolMXBean hikariPoolMXBean = dataSource.getHikariPoolMXBean();

    logger.fine("Starting idle connections: " + hikariPoolMXBean.getIdleConnections());
    logger.fine("Starting active connections: " + hikariPoolMXBean.getActiveConnections());
    logger.fine("Starting total connections: " + hikariPoolMXBean.getTotalConnections());
  }

  private boolean pluginChainIncludesFailoverPlugin(final Properties customProps) {
    final String plugins = customProps.getProperty(PropertyDefinition.PLUGINS.name);
    return plugins.contains("failover");
  }
}
