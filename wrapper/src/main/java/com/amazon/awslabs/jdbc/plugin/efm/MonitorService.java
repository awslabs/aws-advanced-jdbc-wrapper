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

package com.amazon.awslabs.jdbc.plugin.efm;

import com.amazon.awslabs.jdbc.HostSpec;
import java.sql.Connection;
import java.util.Properties;
import java.util.Set;

/**
 * Interface for monitor services. This class implements ways to start and stop monitoring servers
 * when connections are created.
 */
public interface MonitorService {

  MonitorConnectionContext startMonitoring(
      Connection connectionToAbort,
      Set<String> nodeKeys,
      HostSpec hostSpec,
      Properties properties,
      int failureDetectionTimeMillis,
      int failureDetectionIntervalMillis,
      int failureDetectionCount);

  /**
   * Stop monitoring for a connection represented by the given {@link MonitorConnectionContext}.
   * Removes the context from the {@link MonitorImpl}.
   *
   * @param context The {@link MonitorConnectionContext} representing a connection.
   */
  void stopMonitoring(MonitorConnectionContext context);

  /**
   * Stop monitoring the node for all connections represented by the given set of node keys.
   *
   * @param nodeKeys All known references to a server.
   */
  void stopMonitoringForAllConnections(Set<String> nodeKeys);

  void releaseResources();

  /**
   * Handle unused {@link Monitor}.
   *
   * @param monitor The {@link Monitor} in idle.
   */
  void notifyUnused(Monitor monitor);
}
