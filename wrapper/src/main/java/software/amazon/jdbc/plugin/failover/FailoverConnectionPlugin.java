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

package software.amazon.jdbc.plugin.failover;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import software.amazon.jdbc.AwsWrapperProperty;
import software.amazon.jdbc.HostAvailability;
import software.amazon.jdbc.HostListProvider;
import software.amazon.jdbc.HostListProviderService;
import software.amazon.jdbc.HostRole;
import software.amazon.jdbc.HostSpec;
import software.amazon.jdbc.JdbcCallable;
import software.amazon.jdbc.NodeChangeOptions;
import software.amazon.jdbc.OldConnectionSuggestedAction;
import software.amazon.jdbc.PluginManagerService;
import software.amazon.jdbc.PluginService;
import software.amazon.jdbc.hostlistprovider.AuroraHostListProvider;
import software.amazon.jdbc.plugin.AbstractConnectionPlugin;
import software.amazon.jdbc.util.ConnectionUrlParser;
import software.amazon.jdbc.util.Messages;
import software.amazon.jdbc.util.RdsUrlType;
import software.amazon.jdbc.util.RdsUtils;
import software.amazon.jdbc.util.SqlState;
import software.amazon.jdbc.util.SubscribedMethodHelper;
import software.amazon.jdbc.util.Utils;

/**
 * This plugin provides cluster-aware failover features. The plugin switches connections upon
 * detecting communication related exceptions and/or cluster topology changes.
 */
public class FailoverConnectionPlugin extends AbstractConnectionPlugin {

  private static final Logger LOGGER = Logger.getLogger(FailoverConnectionPlugin.class.getName());

  private static final Set<String> subscribedMethods =
      Collections.unmodifiableSet(new HashSet<String>() {
        {
          addAll(SubscribedMethodHelper.NETWORK_BOUND_METHODS);
          add("initHostProvider");
          add("connect");
          add("notifyNodeListChanged");
        }
      });

  static final String METHOD_SET_READ_ONLY = "setReadOnly";
  static final String METHOD_SET_AUTO_COMMIT = "setAutoCommit";
  static final String METHOD_COMMIT = "commit";
  static final String METHOD_ROLLBACK = "rollback";
  private static final String METHOD_GET_AUTO_COMMIT = "getAutoCommit";
  private static final String METHOD_GET_CATALOG = "getCatalog";
  private static final String METHOD_GET_SCHEMA = "getSchema";
  private static final String METHOD_GET_DATABASE = "getDatabase";
  private static final String METHOD_GET_TRANSACTION_ISOLATION = "getTransactionIsolation";
  private static final String METHOD_GET_SESSION_MAX_ROWS = "getSessionMaxRows";
  static final String METHOD_ABORT = "abort";
  static final String METHOD_CLOSE = "close";
  static final String METHOD_IS_CLOSED = "isClosed";
  private final PluginService pluginService;
  protected final Properties properties;
  protected boolean enableFailoverSetting;
  protected int failoverTimeoutMsSetting;
  protected int failoverClusterTopologyRefreshRateMsSetting;
  protected int failoverWriterReconnectIntervalMsSetting;
  protected int failoverReaderConnectTimeoutMsSetting;
  protected boolean explicitlyAutoCommit = true;
  Boolean explicitlyReadOnly = false;
  private boolean closedExplicitly = false;
  protected boolean isClosed = false;
  protected String closedReason = null;
  private final RdsUtils rdsHelper;
  private final ConnectionUrlParser connectionUrlParser;
  protected WriterFailoverHandler writerFailoverHandler = null;
  protected ReaderFailoverHandler readerFailoverHandler = null;
  private Throwable lastExceptionDealtWith = null;
  private PluginManagerService pluginManagerService;
  private boolean isInTransaction = false;
  private RdsUrlType rdsUrlType;

  public static final AwsWrapperProperty FAILOVER_CLUSTER_TOPOLOGY_REFRESH_RATE_MS =
      new AwsWrapperProperty(
          "failoverClusterTopologyRefreshRateMs",
          "2000",
          "Cluster topology refresh rate in millis during a writer failover process. "
              + "During the writer failover process, "
              + "cluster topology may be refreshed at a faster pace than normal to speed up "
              + "discovery of the newly promoted writer.");

  public static final AwsWrapperProperty FAILOVER_TIMEOUT_MS =
      new AwsWrapperProperty(
          "failoverTimeoutMs",
          "300000",
          "Cluster topology refresh rate in millis. "
              + "The cached topology for the cluster will be invalidated after the specified time, "
              + "after which it will be updated during the next interaction with the connection.");

  public static final AwsWrapperProperty FAILOVER_WRITER_RECONNECT_INTERVAL_MS =
      new AwsWrapperProperty(
          "failoverWriterReconnectIntervalMs",
          "2000",
          "Interval of time to wait between attempts to reconnect to a failed writer during a "
              + "writer failover process.");

  public static final AwsWrapperProperty FAILOVER_READER_CONNECT_TIMEOUT_MS =
      new AwsWrapperProperty(
          "failoverReaderConnectTimeoutMs",
          "30000",
          "Reader connection attempt timeout during a reader failover process.");

  public static final AwsWrapperProperty ENABLE_CLUSTER_AWARE_FAILOVER =
      new AwsWrapperProperty(
          "enableClusterAwareFailover", "true",
          "Enable/disable cluster-aware failover logic");

  public FailoverConnectionPlugin(PluginService pluginService, Properties properties) {
    this(pluginService, properties, new RdsUtils(), new ConnectionUrlParser());
  }

  FailoverConnectionPlugin(
      PluginService pluginService,
      Properties properties,
      RdsUtils rdsHelper,
      ConnectionUrlParser parser) {
    this.pluginService = pluginService;
    this.properties = properties;
    this.rdsHelper = rdsHelper;
    this.connectionUrlParser = parser;

    if (pluginService instanceof PluginManagerService) {
      this.pluginManagerService = (PluginManagerService) pluginService;
    }

    initSettings();
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
    if (!this.enableFailoverSetting || canDirectExecute(methodName)) {
      return jdbcMethodFunc.call();
    }

    if (this.isClosed && !allowedOnClosedConnection(methodName)) {
      try {
        invalidInvocationOnClosedConnection();
      } catch (SQLException ex) {
        throw wrapExceptionIfNeeded(exceptionClass, ex);
      }
    }

    T result = null;

    try {
      updateTopology(false);
      result = jdbcMethodFunc.call();
    } catch (IllegalStateException e) {
      dealWithIllegalStateException(e, exceptionClass);
    } catch (Exception e) {
      this.dealWithOriginalException(e, null, exceptionClass);
    }

    try {
      performSpecialMethodHandlingIfRequired(jdbcMethodArgs, methodName);
    } catch (SQLException e) {
      throw wrapExceptionIfNeeded(exceptionClass, e);
    }

    return result;
  }

  @Override
  public void initHostProvider(
      String driverProtocol,
      String initialUrl,
      Properties props,
      HostListProviderService hostListProviderService,
      JdbcCallable<Void, SQLException> initHostProviderFunc)
      throws SQLException {
    initHostProvider(
        driverProtocol,
        initialUrl,
        props,
        hostListProviderService,
        initHostProviderFunc,
        () -> new AuroraHostListProvider(driverProtocol, hostListProviderService, props, initialUrl),
        () ->
            new ClusterAwareReaderFailoverHandler(
                this.pluginService,
                this.properties,
                this.failoverTimeoutMsSetting,
                this.failoverReaderConnectTimeoutMsSetting),
        () ->
            new ClusterAwareWriterFailoverHandler(
                this.pluginService,
                this.readerFailoverHandler,
                this.properties,
                this.failoverTimeoutMsSetting,
                this.failoverClusterTopologyRefreshRateMsSetting,
                this.failoverWriterReconnectIntervalMsSetting));
  }

  void initHostProvider(
      String driverProtocol,
      String initialUrl,
      Properties props,
      HostListProviderService hostListProviderService,
      JdbcCallable<Void, SQLException> initHostProviderFunc,
      Supplier<HostListProvider> hostListProviderSupplier,
      Supplier<ClusterAwareReaderFailoverHandler> readerFailoverHandlerSupplier,
      Supplier<ClusterAwareWriterFailoverHandler> writerFailoverHandlerSupplier)
      throws SQLException {

    if (!this.enableFailoverSetting) {
      return;
    }

    if (hostListProviderService.isStaticHostListProvider()) {
      hostListProviderService.setHostListProvider(hostListProviderSupplier.get());
    }

    this.readerFailoverHandler = readerFailoverHandlerSupplier.get();
    this.writerFailoverHandler = writerFailoverHandlerSupplier.get();

    initHostProviderFunc.call();

    this.rdsUrlType = this.rdsHelper.identifyRdsType(initialUrl);
    if (this.rdsUrlType.isRdsCluster()) {
      this.explicitlyReadOnly = (this.rdsUrlType == RdsUrlType.RDS_READER_CLUSTER);
      LOGGER.finer(
          () -> Messages.get(
              "Failover.parameterValue",
              new Object[] {"explicitlyReadOnly", this.explicitlyReadOnly}));
    }
  }

  @Override
  public OldConnectionSuggestedAction notifyConnectionChanged(EnumSet<NodeChangeOptions> changes) {
    return OldConnectionSuggestedAction.NO_OPINION;
  }

  @Override
  public void notifyNodeListChanged(Map<String, EnumSet<NodeChangeOptions>> changes) {

    if (!this.enableFailoverSetting) {
      return;
    }

    if (LOGGER.isLoggable(Level.FINEST)) {
      StringBuilder sb = new StringBuilder("Changes:");
      for (Map.Entry<String, EnumSet<NodeChangeOptions>> change : changes.entrySet()) {
        if (sb.length() > 0) {
          sb.append("\n");
        }
        sb.append(String.format("\tHost '%s': %s", change.getKey(), change.getValue()));
      }
      LOGGER.finest(sb.toString());
    }

    final HostSpec currentHost = this.pluginService.getCurrentHostSpec();
    final String url = currentHost.getUrl();
    if (isNodeStillValid(url, changes)) {
      return;
    }

    for (String alias : currentHost.getAliases()) {
      if (isNodeStillValid(alias + "/", changes)) {
        return;
      }
    }

    LOGGER.fine(() -> Messages.get("Failover.invalidNode", new Object[] {currentHost}));
  }

  private boolean isNodeStillValid(final String node, Map<String, EnumSet<NodeChangeOptions>> changes) {
    if (changes.containsKey(node)) {
      EnumSet<NodeChangeOptions> options = changes.get(node);
      return !options.contains(NodeChangeOptions.NODE_DELETED) && !options.contains(NodeChangeOptions.WENT_DOWN);
    }
    return true;
  }

  // For testing purposes
  void setRdsUrlType(final RdsUrlType rdsUrlType) {
    this.rdsUrlType = rdsUrlType;
  }

  public boolean isFailoverEnabled() {
    boolean isMultiWriterCluster = this.pluginService.getHosts().stream()
        .filter(h -> h.getRole() == HostRole.WRITER)
        .count() > 1;

    return this.enableFailoverSetting
        && !RdsUrlType.RDS_PROXY.equals(this.rdsUrlType)
        && !Utils.isNullOrEmpty(this.pluginService.getHosts())
        && !isMultiWriterCluster;
  }

  private void initSettings() {
    this.enableFailoverSetting = ENABLE_CLUSTER_AWARE_FAILOVER.getBoolean(this.properties);
    this.failoverTimeoutMsSetting = FAILOVER_TIMEOUT_MS.getInteger(this.properties);
    this.failoverClusterTopologyRefreshRateMsSetting =
        FAILOVER_CLUSTER_TOPOLOGY_REFRESH_RATE_MS.getInteger(this.properties);
    this.failoverWriterReconnectIntervalMsSetting =
        FAILOVER_WRITER_RECONNECT_INTERVAL_MS.getInteger(this.properties);
    this.failoverReaderConnectTimeoutMsSetting =
        FAILOVER_READER_CONNECT_TIMEOUT_MS.getInteger(this.properties);
  }

  private void invalidInvocationOnClosedConnection() throws SQLException {
    if (!this.closedExplicitly) {
      this.isClosed = false;
      this.closedReason = null;
      pickNewConnection();

      // "The active SQL connection has changed. Please re-configure session state if required."
      LOGGER.info(Messages.get("Failover.connectionChangedError"));
      throw new FailoverSuccessSQLException();
    } else {
      String reason = Messages.get("Failover.noOperationsAfterConnectionClosed");
      if (this.closedReason != null) {
        reason += (" " + this.closedReason);
      }

      throw new SQLException(reason, SqlState.CONNECTION_NOT_OPEN.getState());
    }
  }

  private boolean isExplicitlyReadOnly() {
    return this.explicitlyReadOnly != null && this.explicitlyReadOnly;
  }

  /**
   * Checks if there is an underlying connection for this proxy.
   *
   * @return true if there is a connection
   */
  boolean isConnected() {
    return this.pluginService.getCurrentHostSpec().getAvailability() == HostAvailability.AVAILABLE;
  }

  private HostSpec getCurrentWriter() throws SQLException {
    final List<HostSpec> topology = this.pluginService.getHosts();
    if (topology == null) {
      return null;
    }
    return getWriter(topology);
  }

  private HostSpec getWriter(final @NonNull List<HostSpec> hosts) throws SQLException {
    for (HostSpec hostSpec : hosts) {
      if (hostSpec.getRole() == HostRole.WRITER) {
        return hostSpec;
      }
    }
    return null;
  }

  protected void updateTopology(boolean forceUpdate) throws SQLException {
    Connection connection = this.pluginService.getCurrentConnection();
    if (!isFailoverEnabled() || connection == null || connection.isClosed()) {
      return;
    }
    if (forceUpdate) {
      this.pluginService.forceRefreshHostList();
    } else {
      this.pluginService.refreshHostList();
    }
  }

  /**
   * Checks if the given method is allowed on closed connections.
   *
   * @return true if the given method is allowed on closed connections
   */
  private boolean allowedOnClosedConnection(String methodName) {
    return methodName.contains(METHOD_GET_AUTO_COMMIT)
        || methodName.contains(METHOD_GET_CATALOG)
        || methodName.contains(METHOD_GET_SCHEMA)
        || methodName.contains(METHOD_GET_DATABASE)
        || methodName.contains(METHOD_GET_TRANSACTION_ISOLATION)
        || methodName.contains(METHOD_GET_SESSION_MAX_ROWS);
  }

  /**
   * Connects this dynamic failover connection proxy to the host pointed out by the given host
   * index.
   *
   * @param host The host.
   * @throws SQLException if an error occurs
   */
  private void connectTo(HostSpec host) throws SQLException {
    try {
      switchCurrentConnectionTo(host, createConnectionForHost(host));
      LOGGER.fine(
          () -> Messages.get(
              "Failover.establishedConnection",
              new Object[] {host}));
    } catch (SQLException e) {
      if (this.pluginService.getCurrentConnection() != null) {
        StringBuilder msg =
            new StringBuilder("Connection to ")
                .append(isWriter(host) ? "writer" : "reader")
                .append(" host '")
                .append(host.getUrl())
                .append("' failed");
        LOGGER.warning(() -> String.format("%s: %s", msg, e.getMessage()));
      }
      throw e;
    }
  }

  private void connectToWriterIfRequired(Boolean readOnly) throws SQLException {
    if (shouldReconnectToWriter(readOnly) && !Utils.isNullOrEmpty(this.pluginService.getHosts())) {
      try {
        connectTo(getCurrentWriter());
      } catch (SQLException e) {
        failover(getCurrentWriter());
      }
    }
  }

  /**
   * Checks if the given host index points to the primary host.
   *
   * @param hostSpec The current host.
   * @return true if so
   */
  private boolean isWriter(final HostSpec hostSpec) {
    return hostSpec.getRole() == HostRole.WRITER;
  }

  private void performSpecialMethodHandlingIfRequired(Object[] args, String methodName)
      throws SQLException {
    if (methodName.contains(METHOD_SET_AUTO_COMMIT)) {
      this.explicitlyAutoCommit = (Boolean) args[0];
      if (this.pluginManagerService != null) {
        this.pluginManagerService.setInTransaction(!this.explicitlyAutoCommit);
      }
    }

    if (methodName.contains(METHOD_COMMIT) || methodName.contains(METHOD_ROLLBACK)) {
      if (this.pluginManagerService != null) {
        this.pluginManagerService.setInTransaction(false);
      }
    }

    if (methodName.contains(METHOD_SET_READ_ONLY)) {
      this.explicitlyReadOnly = (Boolean) args[0];
      LOGGER.finer(
          () -> Messages.get(
              "Failover.parameterValue",
              new Object[] {"explicitlyReadOnly", this.explicitlyReadOnly}));
      connectToWriterIfRequired(this.explicitlyReadOnly);
    }
  }

  private void processFailoverFailure(String message) throws SQLException {
    LOGGER.severe(message);
    throw new FailoverFailedSQLException(message);
  }

  private boolean shouldAttemptReaderConnection() {
    final List<HostSpec> topology = this.pluginService.getHosts();
    if (topology == null || !isExplicitlyReadOnly()) {
      return false;
    }

    for (HostSpec hostSpec : topology) {
      if (hostSpec.getRole() == HostRole.READER) {
        return true;
      }
    }
    return false;
  }

  boolean shouldPerformWriterFailover() {
    return this.explicitlyReadOnly == null || !this.explicitlyReadOnly;
  }

  private boolean shouldReconnectToWriter(Boolean readOnly) {
    return readOnly != null && !readOnly && !isWriter(this.pluginService.getCurrentHostSpec());
  }

  /**
   * Replaces the previous underlying connection by the connection given. State from previous
   * connection, if any, is synchronized with the new one.
   *
   * @param host The host that matches the given connection.
   * @param connection The connection instance to switch to.
   * @throws SQLException if an error occurs
   */
  private void switchCurrentConnectionTo(HostSpec host, Connection connection) throws SQLException {
    final Connection currentConnection = this.pluginService.getCurrentConnection();
    if (currentConnection != connection) {
      invalidateCurrentConnection();
    }

    boolean readOnly;
    if (isWriter(host)) {
      readOnly = isExplicitlyReadOnly();
    } else if (this.explicitlyReadOnly != null) {
      readOnly = this.explicitlyReadOnly;
    } else if (currentConnection != null) {
      readOnly = currentConnection.isReadOnly();
    } else {
      readOnly = false;
    }
    syncSessionState(currentConnection, connection, readOnly);
    this.pluginService.setCurrentConnection(connection, host);

    if (this.pluginManagerService != null) {
      this.pluginManagerService.setInTransaction(false);
    }
  }

  /**
   * Synchronizes session state between two connections, allowing to override the read-only status.
   *
   * @param source The connection where to get state from.
   * @param target The connection where to set state.
   * @param readOnly The new read-only status.
   * @throws SQLException if an error occurs
   */
  protected void syncSessionState(
      Connection source,
      Connection target,
      boolean readOnly) throws SQLException {
    if (target != null) {
      target.setReadOnly(readOnly);
    }

    if (source == null || target == null) {
      return;
    }

    // TODO: verify if there are other states to sync

    target.setAutoCommit(source.getAutoCommit());
    target.setTransactionIsolation(source.getTransactionIsolation());
  }

  private <E extends Exception> void dealWithOriginalException(
      Throwable originalException,
      Throwable wrapperException,
      Class<E> exceptionClass) throws E {
    Throwable exceptionToThrow = wrapperException;
    if (originalException != null) {
      LOGGER.finer(() -> Messages.get("Failover.detectedException", new Object[] {originalException.getMessage()}));
      if (this.lastExceptionDealtWith != originalException
          && shouldExceptionTriggerConnectionSwitch(originalException)) {
        invalidateCurrentConnection();
        try {
          pickNewConnection();
        } catch (SQLException e) {
          throw wrapExceptionIfNeeded(exceptionClass, e);
        }
        this.lastExceptionDealtWith = originalException;
      }

      if (originalException instanceof Error) {
        throw (Error) originalException;
      }
      exceptionToThrow = originalException;
    }

    if (exceptionToThrow instanceof Error) {
      throw (Error) exceptionToThrow;
    }

    throw wrapExceptionIfNeeded(exceptionClass, exceptionToThrow);
  }

  /**
   * Creates a new physical connection for the given {@link HostSpec}.
   *
   * @param baseHostSpec The host info instance to base the connection off of.
   * @return The new Connection instance.
   * @throws SQLException if an error occurs
   */
  protected Connection createConnectionForHost(HostSpec baseHostSpec) throws SQLException {
    return this.pluginService.connect(baseHostSpec, properties);
  }

  protected <E extends Exception> void dealWithIllegalStateException(
      IllegalStateException e,
      Class<E> exceptionClass) throws E {
    dealWithOriginalException(e.getCause(), e, exceptionClass);
  }

  /**
   * Initiates the failover procedure. This process tries to establish a new connection to an
   * instance in the topology.
   *
   * @param failedHost The host with network errors.
   * @throws SQLException if an error occurs
   */
  protected synchronized void failover(final HostSpec failedHost) throws SQLException {
    if (shouldPerformWriterFailover()) {
      failoverWriter();
    } else {
      failoverReader(failedHost);
    }

    if (isInTransaction || this.pluginService.isInTransaction()) {
      if (this.pluginManagerService != null) {
        this.pluginManagerService.setInTransaction(false);
      }
      // "Transaction resolution unknown. Please re-configure session state if required and try
      // restarting transaction."
      final String errorMessage = Messages.get("Failover.transactionResolutionUnknownError");
      LOGGER.info(errorMessage);
      throw new TransactionStateUnknownSQLException();
    } else {
      // "The active SQL connection has changed due to a connection failure. Please re-configure
      // session state if required. "
      LOGGER.severe(() -> Messages.get("Failover.connectionChangedError"));
      throw new FailoverSuccessSQLException();
    }
  }

  protected void failoverReader(final HostSpec failedHostSpec) throws SQLException {
    LOGGER.fine(() -> Messages.get("Failover.startReaderFailover"));

    HostSpec failedHost = null;
    final Set<String> oldAliases = this.pluginService.getCurrentHostSpec().getAliases();
    if (failedHostSpec != null && failedHostSpec.getAvailability() == HostAvailability.AVAILABLE) {
      failedHost = failedHostSpec;
    }
    ReaderFailoverResult result = readerFailoverHandler.failover(this.pluginService.getHosts(), failedHost);

    if (result != null) {
      final SQLException exception = result.getException();
      if (exception != null) {
        throw exception;
      }
    }

    if (result == null || !result.isConnected()) {
      // "Unable to establish SQL connection to reader instance"
      processFailoverFailure(Messages.get("Failover.unableToConnectToReader"));
      return;
    }

    this.pluginService.setCurrentConnection(result.getConnection(), result.getHost());

    this.pluginService.getCurrentHostSpec().removeAlias(oldAliases.toArray(new String[] {}));
    updateTopology(true);

    LOGGER.fine(
        () -> Messages.get(
            "Failover.establishedConnection",
            new Object[] {this.pluginService.getCurrentHostSpec()}));
  }

  protected void failoverWriter() throws SQLException {
    LOGGER.fine(() -> Messages.get("Failover.startWriterFailover"));
    final HostSpec currentHost = this.pluginService.getCurrentHostSpec();
    final Set<String> oldAliases = this.pluginService.getCurrentHostSpec().getAliases();
    WriterFailoverResult failoverResult = this.writerFailoverHandler.failover(this.pluginService.getHosts());
    if (failoverResult != null) {
      final SQLException exception = failoverResult.getException();
      if (exception != null) {
        throw exception;
      }
    }
    if (failoverResult == null || !failoverResult.isConnected()) {
      // "Unable to establish SQL connection to writer node"
      processFailoverFailure(Messages.get("Failover.unableToConnectToWriter"));
      return;
    }

    // successfully re-connected to a writer node
    final HostSpec writerHostSpec = failoverResult.isNewHost()
        ? getWriter(failoverResult.getTopology())
        : currentHost;
    this.pluginService.setCurrentConnection(failoverResult.getNewConnection(), writerHostSpec);

    LOGGER.fine(
        () -> Messages.get(
            "Failover.establishedConnection",
            new Object[] {this.pluginService.getCurrentHostSpec()}));

    this.pluginService.refreshHostList();
  }

  protected void invalidateCurrentConnection() {
    final Connection conn = this.pluginService.getCurrentConnection();
    if (conn == null) {
      return;
    }

    final HostSpec originalHost = this.pluginService.getCurrentHostSpec();
    if (this.pluginService.isInTransaction()) {
      isInTransaction = this.pluginService.isInTransaction();
      try {
        conn.rollback();
      } catch (SQLException e) {
        // swallow this exception
      }
    }

    try {
      if (!conn.isClosed()) {
        conn.close();
      }
    } catch (SQLException e) {
      // swallow this exception, current connection should be useless anyway.
    }

    try {
      this.pluginService.setCurrentConnection(
          conn,
          new HostSpec(
              originalHost.getHost(),
              originalHost.getPort(),
              originalHost.getRole(),
              HostAvailability.NOT_AVAILABLE));
      this.pluginService.setAvailability(originalHost.getAliases(), HostAvailability.NOT_AVAILABLE);
    } catch (SQLException e) {
      LOGGER.fine(() -> Messages.get("Failover.failedToUpdateCurrentHostspecAvailability"));
    }
  }

  protected synchronized void pickNewConnection() throws SQLException {
    if (this.isClosed && this.closedExplicitly) {
      LOGGER.fine(() -> Messages.get("Failover.transactionResolutionUnknownError"));
      return;
    }

    if (this.pluginService.getCurrentConnection() == null && !shouldAttemptReaderConnection()) {
      try {
        connectTo(getCurrentWriter());
      } catch (SQLException e) {
        failover(getCurrentWriter());
      }
    } else {
      failover(this.pluginService.getCurrentHostSpec());
    }
  }

  protected boolean shouldExceptionTriggerConnectionSwitch(Throwable t) {
    // TODO: support other drivers

    if (!isFailoverEnabled()) {
      LOGGER.fine(() -> Messages.get("Failover.failoverDisabled"));
      return false;
    }

    String sqlState = null;
    if (t instanceof SQLException) {
      sqlState = ((SQLException) t).getSQLState();
    }

    if (sqlState == null) {
      return false;
    }

    return SqlState.isConnectionError(((SQLException) t).getSQLState());
  }

  /**
   * Check whether the method provided can be executed directly without the failover functionality.
   *
   * @param methodName The name of the method that is being called
   * @return true if the method can be executed directly; false otherwise.
   */
  private boolean canDirectExecute(String methodName) {
    return (methodName.contains(METHOD_CLOSE)
        || methodName.contains(METHOD_IS_CLOSED)
        || methodName.contains(METHOD_ABORT));
  }

  /**
   * Check if the throwable is an instance of the given exception and throw it as the required
   * exception class, otherwise throw it as a runtime exception.
   *
   * @param exceptionClass The exception class the exception is expected to be
   * @param exception The exception that occurred while invoking the given method
   * @return an exception indicating the failure that occurred while invoking the given method
   */
  private <E extends Exception> E wrapExceptionIfNeeded(Class<E> exceptionClass, Throwable exception) {
    if (exceptionClass.isAssignableFrom(exception.getClass())) {
      return exceptionClass.cast(exception);
    }
    return exceptionClass.cast(new RuntimeException(exception));
  }

  @Override
  public Connection connect(
      final String driverProtocol,
      final HostSpec hostSpec,
      final Properties props,
      final boolean isInitialConnection,
      final JdbcCallable<Connection, SQLException> connectFunc)
      throws SQLException {

    Connection conn = connectFunc.call();

    if (isInitialConnection) {
      this.pluginService.refreshHostList(conn);
    }

    return conn;
  }

}
