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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import software.amazon.jdbc.AwsWrapperProperty;
import software.amazon.jdbc.HostAvailability;
import software.amazon.jdbc.HostListProviderService;
import software.amazon.jdbc.HostRole;
import software.amazon.jdbc.HostSpec;
import software.amazon.jdbc.dialect.TopologyAwareDatabaseCluster;
import software.amazon.jdbc.util.CacheMap;
import software.amazon.jdbc.util.ConnectionUrlParser;
import software.amazon.jdbc.util.Messages;
import software.amazon.jdbc.util.RdsUrlType;
import software.amazon.jdbc.util.RdsUtils;
import software.amazon.jdbc.util.StringUtils;
import software.amazon.jdbc.util.Utils;

public class AuroraHostListProvider implements DynamicHostListProvider {

  public static final AwsWrapperProperty CLUSTER_TOPOLOGY_REFRESH_RATE_MS =
      new AwsWrapperProperty(
          "clusterTopologyRefreshRateMs",
          "30000",
          "Cluster topology refresh rate in millis. "
              + "The cached topology for the cluster will be invalidated after the specified time, "
              + "after which it will be updated during the next interaction with the connection.");

  public static final AwsWrapperProperty CLUSTER_ID = new AwsWrapperProperty(
      "clusterId", "",
      "A unique identifier for the cluster. "
          + "Connections with the same cluster id share a cluster topology cache. "
          + "If unspecified, a cluster id is automatically created for AWS RDS clusters.");

  public static final AwsWrapperProperty CLUSTER_INSTANCE_HOST_PATTERN =
      new AwsWrapperProperty(
          "clusterInstanceHostPattern",
          null,
          "The cluster instance DNS pattern that will be used to build a complete instance endpoint. "
              + "A \"?\" character in this pattern should be used as a placeholder for cluster instance names. "
              + "This pattern is required to be specified for IP address or custom domain connections to AWS RDS "
              + "clusters. Otherwise, if unspecified, the pattern will be automatically created for AWS RDS clusters.");
  // TODO: move to dialect
  private static final String PG_IS_READER_QUERY = "SELECT pg_is_in_recovery() AS is_reader";
  private static final String MYSQL_IS_READER_QUERY = "SELECT @@innodb_read_only AS is_reader";
  private static final String IS_READER_COLUMN = "is_reader";

  private final HostListProviderService hostListProviderService;
  private final String originalUrl;
  private RdsUrlType rdsUrlType;
  private final RdsUtils rdsHelper;

  private long refreshRateNano = CLUSTER_TOPOLOGY_REFRESH_RATE_MS.defaultValue != null
      ? TimeUnit.MILLISECONDS.toNanos(Long.parseLong(CLUSTER_TOPOLOGY_REFRESH_RATE_MS.defaultValue))
      : TimeUnit.MILLISECONDS.toNanos(30000);
  private final long suggestedClusterIdRefreshRateNano = TimeUnit.MINUTES.toNanos(10);
  private List<HostSpec> hostList = new ArrayList<>();
  private List<HostSpec> lastReturnedHostList;
  private List<HostSpec> initialHostList = new ArrayList<>();
  private HostSpec initialHostSpec;

  public static final CacheMap<String, List<HostSpec>> topologyCache = new CacheMap<>();
  public static final CacheMap<String, String> suggestedPrimaryClusterIdCache = new CacheMap<>();
  public static final CacheMap<String, Boolean> primaryClusterIdCache = new CacheMap<>();

  private final ReentrantLock lock = new ReentrantLock();
  protected String clusterId;
  protected HostSpec clusterInstanceTemplate;
  protected ConnectionUrlParser connectionUrlParser;
  protected TopologyAwareDatabaseCluster topologyAwareDialect;

  // A primary clusterId is a clusterId that is based off of a cluster endpoint URL
  // (rather than a GUID or a value provided by the user).
  protected boolean isPrimaryClusterId;

  protected boolean isInitialized = false;

  private static final Logger LOGGER = Logger.getLogger(AuroraHostListProvider.class.getName());

  Properties properties;

  public AuroraHostListProvider(
      final String driverProtocol,
      final HostListProviderService hostListProviderService,
      final Properties properties,
      final String originalUrl) {
    this(driverProtocol, hostListProviderService, properties, originalUrl, new ConnectionUrlParser());
  }

  public AuroraHostListProvider(
      final String driverProtocol,
      final HostListProviderService hostListProviderService,
      final Properties properties,
      final String originalUrl,
      final ConnectionUrlParser connectionUrlParser) {
    this.rdsHelper = new RdsUtils();
    this.hostListProviderService = hostListProviderService;
    this.properties = properties;
    this.originalUrl = originalUrl;
    this.connectionUrlParser = connectionUrlParser;
  }

  protected void init() throws SQLException {
    if (this.isInitialized) {
      return;
    }

    lock.lock();
    try {
      if (this.isInitialized) {
        return;
      }

      // initial topology is based on connection string
      this.initialHostList =
          this.connectionUrlParser.getHostsFromConnectionUrl(this.originalUrl, false);
      if (this.initialHostList == null || this.initialHostList.isEmpty()) {
        throw new SQLException(Messages.get("AuroraHostListProvider.parsedListEmpty",
            new Object[] {this.originalUrl}));
      }
      this.initialHostSpec = this.initialHostList.get(0);
      this.hostListProviderService.setInitialConnectionHostSpec(this.initialHostSpec);

      this.clusterId = UUID.randomUUID().toString();
      this.isPrimaryClusterId = false;
      this.refreshRateNano =
          TimeUnit.MILLISECONDS.toNanos(CLUSTER_TOPOLOGY_REFRESH_RATE_MS.getInteger(properties));
      this.clusterInstanceTemplate =
          CLUSTER_INSTANCE_HOST_PATTERN.getString(this.properties) == null
              ? new HostSpec(rdsHelper.getRdsInstanceHostPattern(originalUrl))
              : new HostSpec(CLUSTER_INSTANCE_HOST_PATTERN.getString(this.properties));
      validateHostPatternSetting(this.clusterInstanceTemplate.getHost());

      this.rdsUrlType = rdsHelper.identifyRdsType(originalUrl);

      final String clusterIdSetting = CLUSTER_ID.getString(this.properties);
      if (!StringUtils.isNullOrEmpty(clusterIdSetting)) {
        this.clusterId = clusterIdSetting;
      } else if (rdsUrlType == RdsUrlType.RDS_PROXY) {
        // Each proxy is associated with a single cluster, so it's safe to use RDS Proxy Url as cluster
        // identification
        this.clusterId = this.initialHostSpec.getUrl();
      } else if (rdsUrlType.isRds()) {
        final ClusterSuggestedResult clusterSuggestedResult =
            getSuggestedClusterId(this.initialHostSpec.getUrl());
        if (clusterSuggestedResult != null && !StringUtils.isNullOrEmpty(
            clusterSuggestedResult.clusterId)) {
          this.clusterId = clusterSuggestedResult.clusterId;
          this.isPrimaryClusterId = clusterSuggestedResult.isPrimaryClusterId;
        } else {
          final String clusterRdsHostUrl =
              this.rdsHelper.getRdsClusterHostUrl(this.initialHostSpec.getUrl());
          if (!StringUtils.isNullOrEmpty(clusterRdsHostUrl)) {
            this.clusterId = this.clusterInstanceTemplate.isPortSpecified()
                ? String.format("%s:%s", clusterRdsHostUrl, this.clusterInstanceTemplate.getPort())
                : clusterRdsHostUrl;
            this.isPrimaryClusterId = true;
            primaryClusterIdCache.put(this.clusterId, true, this.suggestedClusterIdRefreshRateNano);
          }
        }
      }

      this.isInitialized = true;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get cluster topology. It may require an extra call to database to fetch the latest topology. A
   * cached copy of topology is returned if it's not yet outdated (controlled by {@link
   * #refreshRateNano}).
   *
   * @param conn A connection to database to fetch the latest topology, if needed.
   * @param forceUpdate If true, it forces a service to ignore cached copy of topology and to fetch
   *     a fresh one.
   * @return a list of hosts that describes cluster topology. A writer is always at position 0.
   *     Returns an empty list if isn't available or is invalid (doesn't contain a writer).
   * @throws SQLException if errors occurred while retrieving the topology.
   */
  public FetchTopologyResult getTopology(final Connection conn, final boolean forceUpdate) throws SQLException {
    init();

    final String suggestedPrimaryClusterId = suggestedPrimaryClusterIdCache.get(this.clusterId);

    // Change clusterId by accepting a suggested one
    if (!StringUtils.isNullOrEmpty(suggestedPrimaryClusterId)
        && !this.clusterId.equals(suggestedPrimaryClusterId)) {

      this.clusterId = suggestedPrimaryClusterId;
      this.isPrimaryClusterId = true;
    }

    final List<HostSpec> cachedHosts = topologyCache.get(this.clusterId);

    // This clusterId is a primary one and is about to create a new entry in the cache.
    // When a primary entry is created it needs to be suggested for other (non-primary) entries.
    // Remember a flag to do suggestion after cache is updated.
    final boolean needToSuggest = cachedHosts == null && this.isPrimaryClusterId;

    if (cachedHosts == null || forceUpdate) {

      // need to re-fetch topology

      if (conn == null) {
        // can't fetch the latest topology since no connection
        // return original hosts parsed from connection string
        return new FetchTopologyResult(false, this.initialHostList);
      }

      // fetch topology from the DB
      final List<HostSpec> hosts = queryForTopology(conn);

      if (!Utils.isNullOrEmpty(hosts)) {
        topologyCache.put(this.clusterId, hosts, this.refreshRateNano);
        if (needToSuggest) {
          this.suggestPrimaryCluster(hosts);
        }
        return new FetchTopologyResult(false, hosts);
      }
    }

    if (cachedHosts == null) {
      return new FetchTopologyResult(false, this.initialHostList);
    } else {
      // use cached data
      return new FetchTopologyResult(true, cachedHosts);
    }
  }

  private ClusterSuggestedResult getSuggestedClusterId(final String url) {
    for (final Entry<String, List<HostSpec>> entry : topologyCache.getEntries().entrySet()) {
      final String key = entry.getKey(); // clusterId
      final List<HostSpec> hosts = entry.getValue();
      final boolean isPrimaryCluster = primaryClusterIdCache.get(key, false,
          this.suggestedClusterIdRefreshRateNano);
      if (key.equals(url)) {
        return new ClusterSuggestedResult(url, isPrimaryCluster);
      }
      if (hosts == null) {
        continue;
      }
      for (final HostSpec host : hosts) {
        if (host.getUrl().equals(url)) {
          LOGGER.finest(() -> Messages.get("AuroraHostListProvider.suggestedClusterId",
              new Object[]{key, url}));
          return new ClusterSuggestedResult(key, isPrimaryCluster);
        }
      }
    }
    return null;
  }

  protected void suggestPrimaryCluster(final @NonNull List<HostSpec> primaryClusterHosts) {
    if (Utils.isNullOrEmpty(primaryClusterHosts)) {
      return;
    }

    final Set<String> primaryClusterHostUrls = new HashSet<>();
    for (final HostSpec hostSpec : primaryClusterHosts) {
      primaryClusterHostUrls.add(hostSpec.getUrl());
    }

    for (final Entry<String, List<HostSpec>> entry : topologyCache.getEntries().entrySet()) {
      final String clusterId = entry.getKey();
      final List<HostSpec> clusterHosts = entry.getValue();
      final boolean isPrimaryCluster = primaryClusterIdCache.get(clusterId, false,
          this.suggestedClusterIdRefreshRateNano);
      final String suggestedPrimaryClusterId = suggestedPrimaryClusterIdCache.get(clusterId);
      if (isPrimaryCluster
          || !StringUtils.isNullOrEmpty(suggestedPrimaryClusterId)
          || Utils.isNullOrEmpty(clusterHosts)) {
        continue;
      }

      // The entry is non-primary
      for (final HostSpec host : clusterHosts) {
        if (primaryClusterHostUrls.contains(host.getUrl())) {
          // Instance on this cluster matches with one of the instance on primary cluster
          // Suggest the primary clusterId to this entry
          suggestedPrimaryClusterIdCache.put(clusterId, this.clusterId,
              this.suggestedClusterIdRefreshRateNano);
          break;
        }
      }
    }
  }

  /**
   * Obtain a cluster topology from database.
   *
   * @param conn A connection to database to fetch the latest topology.
   * @return a list of {@link HostSpec} objects representing the topology
   * @throws SQLException if errors occurred while retrieving the topology.
   */
  protected List<HostSpec> queryForTopology(final Connection conn) throws SQLException {

    if (this.topologyAwareDialect == null) {
      if (!(this.hostListProviderService.getDialect() instanceof TopologyAwareDatabaseCluster)) {
        LOGGER.warning(() -> Messages.get("AuroraHostListProvider.invalidDialect"));
        return null;
      }
      this.topologyAwareDialect = (TopologyAwareDatabaseCluster) this.hostListProviderService.getDialect();
    }

    try (final Statement stmt = conn.createStatement();
        final ResultSet resultSet = stmt.executeQuery(this.topologyAwareDialect.getTopologyQuery())) {
      return processQueryResults(resultSet);
    } catch (final SQLSyntaxErrorException e) {
      throw new SQLException(Messages.get("AuroraHostListProvider.invalidQuery"), e);
    }
  }

  /**
   * Form a list of hosts from the results of the topology query.
   *
   * @param resultSet The results of the topology query
   * @return a list of {@link HostSpec} objects representing
   *     the topology that was returned by the
   *     topology query. The list will be empty if the topology query returned an invalid topology
   *     (no writer instance).
   */
  private List<HostSpec> processQueryResults(final ResultSet resultSet) throws SQLException {

    final HashMap<String, HostSpec> hostMap = new HashMap<>();

    // Data is result set is ordered by last updated time so the latest records go last.
    // When adding hosts to a map, the newer records replace the older ones.
    while (resultSet.next()) {
      final HostSpec host = createHost(resultSet);
      hostMap.put(host.getHost(), host);
    }

    int writerCount = 0;
    final List<HostSpec> hosts = new ArrayList<>();

    for (final HostSpec host : hostMap.values()) {
      if (host.getRole() == HostRole.WRITER) {
        writerCount++;
      }
      hosts.add(host);
    }

    if (writerCount == 0) {
      LOGGER.severe(
          () -> Messages.get(
              "AuroraHostListProvider.invalidTopology"));
      hosts.clear();
    }
    return hosts;
  }

  /**
   * Creates an instance of HostSpec which captures details about a connectable host.
   *
   * @param resultSet the result set from querying the topology
   * @return a {@link HostSpec} instance for a specific instance from the cluster
   * @throws SQLException If unable to retrieve the hostName from the result set
   */
  private HostSpec createHost(final ResultSet resultSet) throws SQLException {
    // According to TopologyAwareDatabaseCluster.getTopologyQuery() the result set
    // should contain 4 columns: node ID, 1/0 (writer/reader), CPU utilization, node lag in time.
    String hostName = resultSet.getString(1);
    final boolean isWriter = resultSet.getBoolean(2);
    final float cpuUtilization = resultSet.getFloat(3);
    final float nodeLag = resultSet.getFloat(4);

    // Calculate weight based on node lag in time and CPU utilization.
    final long weight = Math.round(nodeLag) * 100L + Math.round(cpuUtilization);

    hostName = hostName == null ? "?" : hostName;
    final String endpoint = getHostEndpoint(hostName);
    final int port = this.clusterInstanceTemplate.isPortSpecified()
        ? this.clusterInstanceTemplate.getPort()
        : this.initialHostSpec.getPort();

    final HostSpec hostSpec = new HostSpec(
        endpoint,
        port,
        isWriter ? HostRole.WRITER : HostRole.READER,
        HostAvailability.AVAILABLE,
        weight);
    hostSpec.addAlias(hostName);
    return hostSpec;
  }

  /**
   * Build a host dns endpoint based on host/node name.
   *
   * @param nodeName A host name.
   * @return Host dns endpoint
   */
  private String getHostEndpoint(final String nodeName) {
    final String host = this.clusterInstanceTemplate.getHost();
    return host.replace("?", nodeName);
  }

  /**
   * Get cached topology.
   *
   * @return list of hosts that represents topology. If there's no topology in the cache or the
   *     cached topology is outdated, it returns null.
   */
  public @Nullable List<HostSpec> getCachedTopology() {
    return topologyCache.get(this.clusterId);
  }

  /**
   * Clear topology cache for all clusters.
   */
  public static void clearAll() {
    topologyCache.clear();
    primaryClusterIdCache.clear();
    suggestedPrimaryClusterIdCache.clear();
  }

  /**
   * Clear topology cache for the current cluster.
   */
  public void clear() {
    topologyCache.remove(this.clusterId);
  }

  @Override
  public List<HostSpec> refresh() throws SQLException {
    return this.refresh(null);
  }

  @Override
  public List<HostSpec> refresh(final Connection connection) throws SQLException {
    init();
    final Connection currentConnection = connection != null
        ? connection
        : this.hostListProviderService.getCurrentConnection();

    final FetchTopologyResult results = getTopology(currentConnection, false);
    LOGGER.finest(() -> Utils.logTopology(results.hosts));

    if (results.isCachedData && this.lastReturnedHostList == results.hosts) {
      return null; // no topology update
    }

    this.hostList = results.hosts;
    this.lastReturnedHostList = this.hostList;
    return Collections.unmodifiableList(hostList);
  }

  @Override
  public List<HostSpec> forceRefresh() throws SQLException {
    return this.forceRefresh(null);
  }

  @Override
  public List<HostSpec> forceRefresh(final Connection connection) throws SQLException {
    init();
    final Connection currentConnection = connection != null
        ? connection
        : this.hostListProviderService.getCurrentConnection();

    final FetchTopologyResult results = getTopology(currentConnection, true);
    LOGGER.finest(() -> Utils.logTopology(results.hosts));
    this.hostList = results.hosts;
    this.lastReturnedHostList = this.hostList;
    return Collections.unmodifiableList(this.hostList);
  }

  public RdsUrlType getRdsUrlType() throws SQLException {
    init();
    return this.rdsUrlType;
  }

  private void validateHostPatternSetting(final String hostPattern) {
    if (!this.rdsHelper.isDnsPatternValid(hostPattern)) {
      // "Invalid value for the 'clusterInstanceHostPattern' configuration setting - the host
      // pattern must contain a '?'
      // character as a placeholder for the DB instance identifiers of the instances in the cluster"
      final String message = Messages.get("AuroraHostListProvider.invalidPattern");
      LOGGER.severe(message);
      throw new RuntimeException(message);
    }

    final RdsUrlType rdsUrlType = this.rdsHelper.identifyRdsType(hostPattern);
    if (rdsUrlType == RdsUrlType.RDS_PROXY) {
      // "An RDS Proxy url can't be used as the 'clusterInstanceHostPattern' configuration setting."
      final String message =
          Messages.get("AuroraHostListProvider.clusterInstanceHostPatternNotSupportedForRDSProxy");
      LOGGER.severe(message);
      throw new RuntimeException(message);
    }

    if (rdsUrlType == RdsUrlType.RDS_CUSTOM_CLUSTER) {
      // "An RDS Custom Cluster endpoint can't be used as the 'clusterInstanceHostPattern'
      // configuration setting."
      final String message =
          Messages.get("AuroraHostListProvider.clusterInstanceHostPatternNotSupportedForRdsCustom");
      LOGGER.severe(message);
      throw new RuntimeException(message);
    }
  }

  public static void logCache() {
    LOGGER.finest(() -> {
      final StringBuilder sb = new StringBuilder();
      final Set<Entry<String, List<HostSpec>>> cacheEntries = topologyCache.getEntries().entrySet();

      if (cacheEntries.isEmpty()) {
        sb.append("Cache is empty.");
        return sb.toString();
      }

      for (final Entry<String, List<HostSpec>> entry : cacheEntries) {
        final List<HostSpec> hosts = entry.getValue();
        final Boolean isPrimaryCluster = primaryClusterIdCache.get(entry.getKey());
        final String suggestedPrimaryClusterId = suggestedPrimaryClusterIdCache.get(entry.getKey());

        if (sb.length() > 0) {
          sb.append("\n");
        }
        sb.append("[").append(entry.getKey()).append("]:\n")
            .append("\tisPrimaryCluster: ")
            .append(isPrimaryCluster != null && isPrimaryCluster).append("\n")
            .append("\tsuggestedPrimaryCluster: ")
            .append(suggestedPrimaryClusterId).append("\n")
            .append("\tHosts: ");

        if (hosts == null) {
          sb.append("<null>");
        } else {
          for (final HostSpec h : hosts) {
            sb.append("\n\t").append(h);
          }
        }
      }
      return sb.toString();
    });
  }

  static class FetchTopologyResult {

    public List<HostSpec> hosts;
    public boolean isCachedData;

    public FetchTopologyResult(final boolean isCachedData, final List<HostSpec> hosts) {
      this.isCachedData = isCachedData;
      this.hosts = hosts;
    }
  }

  static class ClusterSuggestedResult {
    public String clusterId;
    public boolean isPrimaryClusterId;

    public ClusterSuggestedResult(final String clusterId, final boolean isPrimaryClusterId) {
      this.clusterId = clusterId;
      this.isPrimaryClusterId = isPrimaryClusterId;
    }
  }

  @Override
  public HostRole getHostRole(Connection conn) throws SQLException {
    try (final Statement stmt = conn.createStatement();
         final ResultSet rs = stmt.executeQuery(this.topologyAwareDialect.getIsReaderQuery())) {
      if (rs.next()) {
        boolean isReader = rs.getBoolean(IS_READER_COLUMN);
        return isReader ? HostRole.READER : HostRole.WRITER;
      }
    } catch (SQLException e) {
      throw new SQLException(Messages.get("AuroraHostListProvider.errorGettingHostRole"), e);
    }

    throw new SQLException(Messages.get("AuroraHostListProvider.errorGettingHostRole"));
  }
}
