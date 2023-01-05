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

package software.amazon.jdbc.benchmarks;

import static org.junit.jupiter.api.Assertions.fail;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import software.amazon.jdbc.Driver;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.postgresql.PGProperty;
import software.amazon.jdbc.PropertyDefinition;
import software.amazon.jdbc.plugin.readwritesplitting.ReadWriteSplittingPlugin;

@State(Scope.Benchmark)
public class ReadWriteSplittingLoadBenchmarks {

  // User configures connection properties here
  public static final String POSTGRESQL_CONNECTION_STRING =
      "jdbc:aws-wrapper:postgresql://test-db.cluster-XYZ.us-east-2.rds.amazonaws.com:5432/readWriteSplittingExample";
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";

  private static final int NUM_THREADS = 10;
  protected static final String QUERY_1 = "select " +
      "l_returnflag, " +
      "l_linestatus, " +
      "sum(l_quantity) as sum_qty, " +
      "sum(l_extendedprice) as sum_base_price, " +
      "sum(l_extendedprice * (1 - l_discount)) as sum_disc_price, " +
      "sum(l_extendedprice * (1 - l_discount) * (1 + l_tax)) as sum_charge, " +
      "avg(l_quantity) as avg_qty, " +
      "avg(l_extendedprice) as avg_price, " +
      "avg(l_discount) as avg_disc, " +
      "count(*) as count_order " +
      "from " +
      "LINEITEM " +
      "where " +
      "l_shipdate <= date '1998-12-01' - interval '110' day " +
      "group by " +
      "l_returnflag, " +
      "l_linestatus " +
      "order by " +
      "l_returnflag, " +
      "l_linestatus;";

  @Setup(Level.Iteration)
  public static void setUp() throws SQLException {
    if (!org.postgresql.Driver.isRegistered()) {
      org.postgresql.Driver.register();
    }

    if (!Driver.isRegistered()) {
      Driver.register();
    }
  }

  protected static Properties initNoPluginPropsWithTimeouts() {
    final Properties props = new Properties();
    props.setProperty(PGProperty.USER.getName(), USERNAME);
    props.setProperty(PGProperty.PASSWORD.getName(), PASSWORD);
    props.setProperty(PGProperty.TCP_KEEP_ALIVE.getName(), Boolean.FALSE.toString());
    props.setProperty(PGProperty.CONNECT_TIMEOUT.getName(), "5");
    props.setProperty(PGProperty.SOCKET_TIMEOUT.getName(), "5");

    return props;
  }

  protected static Properties initReadWritePluginProps() {
    final Properties props = initNoPluginPropsWithTimeouts();
    props.setProperty(PropertyDefinition.PLUGINS.name, "auroraHostList,readWriteSplitting");
    return props;
  }

  protected static Properties initReadWritePluginLoadBalancingProps() {
    final Properties props = initNoPluginPropsWithTimeouts();
    props.setProperty(PropertyDefinition.PLUGINS.name, "auroraHostList,readWriteSplitting");
    props.setProperty(ReadWriteSplittingPlugin.LOAD_BALANCE_READ_ONLY_TRAFFIC.name, "true");
    return props;
  }

  protected static Connection connectToInstance(String instanceUrl, Properties props)
      throws SQLException {
    return DriverManager.getConnection(instanceUrl, props);
  }

  @Benchmark
  public void noPluginEnabledBenchmarkTest() throws SQLException {
    final List<Thread> connectionsList = new ArrayList<>(NUM_THREADS);

    for (int i = 0; i < NUM_THREADS; i++) {
      connectionsList.add(getThread_PGReadWriteSplitting(initNoPluginPropsWithTimeouts()));
    }

    // begin all connections
    for (Thread thread : connectionsList) {
      thread.start();
    }

    // stop all connections
    for (Thread thread : connectionsList){
      thread.interrupt();
    }
  }

  @Benchmark
  public void readWriteSplittingPluginEnabledBenchmarkTest() throws SQLException {
    final List<Thread> connectionsList = new ArrayList<>(NUM_THREADS);

    for (int i = 0; i < NUM_THREADS; i++) {
      connectionsList.add(getThread_PGReadWriteSplitting(initReadWritePluginProps()));
    }

    // begin all connections
    for (Thread thread : connectionsList) {
      thread.start();
    }

    // stop all connections
    for (Thread thread : connectionsList){
      thread.interrupt();
    }
  }

  @Benchmark
  public void readWriteSplittingPluginLoadBalancingEnabledBenchmarkTest() throws SQLException {
    final List<Thread> connectionsList = new ArrayList<>(NUM_THREADS);

    for (int i = 0; i < NUM_THREADS; i++) {
      connectionsList.add(getThread_PGReadWriteSplitting(initReadWritePluginLoadBalancingProps()));
    }

    // begin all connections
    for (Thread thread : connectionsList) {
      thread.start();
    }

    // stop all connections
    for (Thread thread : connectionsList){
      thread.interrupt();
    }
  }

  private Thread getThread_PGReadWriteSplitting(Properties props) {
    return new Thread(() -> {
      try {
        Connection conn = connectToInstance(POSTGRESQL_CONNECTION_STRING, props);

        Thread.sleep(5000);

        // Execute long query
        final Statement statement = conn.createStatement();
        statement.executeQuery(QUERY_1);

        try (final ResultSet result = statement.executeQuery(QUERY_1)) {
          fail("Sleep query finished, should not be possible with the network down.");
        }
      } catch (InterruptedException interruptedException) {
        // Ignore, stop the thread
      } catch (Exception exception) {
      }
    });
  }

  public static void main(String[] args) throws Exception {
    org.openjdk.jmh.Main.main(args);
  }
}
