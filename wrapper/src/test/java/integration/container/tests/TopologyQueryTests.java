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

package integration.container.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import integration.DatabaseEngineDeployment;
import integration.DriverHelper;
import integration.TestEnvironmentFeatures;
import integration.container.ConnectionStringHelper;
import integration.container.TestDriver;
import integration.container.TestDriverProvider;
import integration.container.TestEnvironment;
import integration.container.condition.DisableOnTestFeature;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.jdbc.dialect.AuroraMysqlDialect;
import software.amazon.jdbc.dialect.AuroraPgDialect;
import software.amazon.jdbc.dialect.RdsMultiAzDbClusterMysqlDialect;
import software.amazon.jdbc.dialect.RdsMultiAzDbClusterPgDialect;

@TestMethodOrder(MethodName.class)
@DisableOnTestFeature({
    TestEnvironmentFeatures.PERFORMANCE,
    TestEnvironmentFeatures.RUN_HIBERNATE_TESTS_ONLY,
    TestEnvironmentFeatures.RUN_AUTOSCALING_TESTS_ONLY})
public class TopologyQueryTests {
  private static final Logger LOGGER = Logger.getLogger(BasicConnectivityTests.class.getName());

  @TestTemplate
  @ExtendWith(TestDriverProvider.class)
  public void auroraTestTypes(TestDriver testDriver) throws SQLException {
    LOGGER.info(testDriver.toString());

    // Topology queries fail on docker containers, can't test topology for them
    if (TestEnvironment.getCurrent().getInfo().getRequest().getDatabaseEngineDeployment()
        == DatabaseEngineDeployment.DOCKER) {
      return;
    }

    final Properties props = ConnectionStringHelper.getDefaultPropertiesWithNoPlugins();
    DriverHelper.setConnectTimeout(testDriver, props, 10, TimeUnit.SECONDS);
    DriverHelper.setSocketTimeout(testDriver, props, 10, TimeUnit.SECONDS);

    String url =
        ConnectionStringHelper.getWrapperUrl(
            testDriver,
            TestEnvironment.getCurrent()
                .getInfo()
                .getDatabaseInfo()
                .getInstances()
                .get(0)
                .getHost(),
            TestEnvironment.getCurrent()
                .getInfo()
                .getDatabaseInfo()
                .getInstances()
                .get(0)
                .getPort(),
            TestEnvironment.getCurrent().getInfo().getDatabaseInfo().getDefaultDbName());
    LOGGER.finest("Connecting to " + url);

    String query = null;
    if (TestEnvironment.getCurrent().getCurrentDriver() == TestDriver.PG) {
      query = AuroraPgDialect.TOPOLOGY_QUERY;
    } else {
      query = AuroraMysqlDialect.TOPOLOGY_QUERY;
    }

    final Connection conn = DriverManager.getConnection(url, props);
    assertTrue(conn.isValid(5));
    Statement stmt = conn.createStatement();
    stmt.executeQuery(query);
    ResultSet rs = stmt.getResultSet();
    int cols = rs.getMetaData().getColumnCount();
    List<String> columnTypes = new ArrayList<>();
    List<String> expectedTypes = Arrays.asList(
        "VARCHAR",
        "BIGINT",
        "DOUBLE",
        "DOUBLE",
        "DATETIME"
    );
    for (int i = 1; i <= cols; i++) {
      columnTypes.add(rs.getMetaData().getColumnTypeName(i));
    }
    assertEquals(columnTypes, expectedTypes);
    conn.close();
  }

  @TestTemplate
  @ExtendWith(TestDriverProvider.class)
  public void auroraTestTimestamp(TestDriver testDriver) throws SQLException, ParseException {
    LOGGER.info(testDriver.toString());

    // Topology queries fail on docker containers, can't test topology for them
    if (TestEnvironment.getCurrent().getInfo().getRequest().getDatabaseEngineDeployment()
        == DatabaseEngineDeployment.DOCKER) {
      return;
    }

    final Properties props = ConnectionStringHelper.getDefaultPropertiesWithNoPlugins();
    DriverHelper.setConnectTimeout(testDriver, props, 10, TimeUnit.SECONDS);
    DriverHelper.setSocketTimeout(testDriver, props, 10, TimeUnit.SECONDS);

    String url =
        ConnectionStringHelper.getWrapperUrl(
            testDriver,
            TestEnvironment.getCurrent()
                .getInfo()
                .getDatabaseInfo()
                .getInstances()
                .get(0)
                .getHost(),
            TestEnvironment.getCurrent()
                .getInfo()
                .getDatabaseInfo()
                .getInstances()
                .get(0)
                .getPort(),
            TestEnvironment.getCurrent().getInfo().getDatabaseInfo().getDefaultDbName());
    LOGGER.finest("Connecting to " + url);

    String query = null;
    if (TestEnvironment.getCurrent().getCurrentDriver() == TestDriver.PG) {
      query = AuroraPgDialect.TOPOLOGY_QUERY;
    } else {
      query = AuroraMysqlDialect.TOPOLOGY_QUERY;
    }

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");

    final Connection conn = DriverManager.getConnection(url, props);
    assertTrue(conn.isValid(5));
    Statement stmt = conn.createStatement();
    stmt.executeQuery(query);
    ResultSet rs = stmt.getResultSet();

    Date date;
    while (rs.next()) {
      date = format.parse(rs.getString("LAST_UPDATE_TIMESTAMP"));
      assertNotNull(date);
    }

    conn.close();
  }

  @TestTemplate
  @ExtendWith(TestDriverProvider.class)
  @Disabled
  // Disabled due to RDS integration tests not being supported yet
  public void rdsTestTypes(TestDriver testDriver) throws SQLException {
    LOGGER.info(testDriver.toString());

    // Topology queries fail on docker containers, can't test topology for them
    if (TestEnvironment.getCurrent().getInfo().getRequest().getDatabaseEngineDeployment()
        == DatabaseEngineDeployment.DOCKER) {
      return;
    }

    final Properties props = ConnectionStringHelper.getDefaultPropertiesWithNoPlugins();
    DriverHelper.setConnectTimeout(testDriver, props, 10, TimeUnit.SECONDS);
    DriverHelper.setSocketTimeout(testDriver, props, 10, TimeUnit.SECONDS);

    String url =
        ConnectionStringHelper.getWrapperUrl(
            testDriver,
            TestEnvironment.getCurrent()
                .getInfo()
                .getDatabaseInfo()
                .getInstances()
                .get(0)
                .getHost(),
            TestEnvironment.getCurrent()
                .getInfo()
                .getDatabaseInfo()
                .getInstances()
                .get(0)
                .getPort(),
            TestEnvironment.getCurrent().getInfo().getDatabaseInfo().getDefaultDbName());
    LOGGER.finest("Connecting to " + url);
    List<String> expectedTypes;
    String query = null;
    if (TestEnvironment.getCurrent().getCurrentDriver() == TestDriver.PG) {
      query = RdsMultiAzDbClusterPgDialect.TOPOLOGY_QUERY;
      expectedTypes = Arrays.asList(
          "text",
          "text",
          "int4"
      );
    } else {
      query = RdsMultiAzDbClusterMysqlDialect.TOPOLOGY_QUERY;
      expectedTypes = Arrays.asList(
          "INT",
          "VARCHAR",
          "INT"
      );
    }

    final Connection conn = DriverManager.getConnection(url, props);
    assertTrue(conn.isValid(5));
    Statement stmt = conn.createStatement();
    stmt.executeQuery(query);
    ResultSet rs = stmt.getResultSet();
    int cols = rs.getMetaData().getColumnCount();
    List<String> columnTypes = new ArrayList<>();

    for (int i = 1; i <= cols; i++) {
      columnTypes.add(rs.getMetaData().getColumnTypeName(i));
    }
    assertEquals(columnTypes, expectedTypes);
    conn.close();
  }
}