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

package software.amazon.jdbc.authentication;

import software.amazon.jdbc.HostSpec;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import java.util.Properties;

public class AwsCredentialsManager {
  private static AwsCredentialsProviderHandler handler = null;

  public static synchronized void setCustomHandler(AwsCredentialsProviderHandler customHandler) {
    handler = customHandler;
  }

  public static synchronized void resetCustomHandler() {
    handler = null;
  }

  public static synchronized AwsCredentialsProvider getProvider(HostSpec hostSpec,
      Properties props) {
    return handler != null ? handler.getAwsCredentialsProvider(hostSpec, props)
        : getDefaultProvider();
  }

  private static AwsCredentialsProvider getDefaultProvider() {
    return DefaultCredentialsProvider.create();
  }
}
