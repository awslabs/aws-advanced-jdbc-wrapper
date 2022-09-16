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

package integration.container.standard.mariadb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;
import java.util.Random;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import software.amazon.jdbc.PropertyDefinition;

public class SpringTests extends StandardMariadbBaseTest {

  @Test
  public void testOpenConnection() {

    JdbcTemplate jdbcTemplate = new JdbcTemplate(mariadbDataSource());

    int rnd = new Random().nextInt(100);
    int result = jdbcTemplate.queryForObject("SELECT " + rnd, Integer.class);
    assertEquals(rnd, result);
  }

  private DataSource mariadbDataSource() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("software.amazon.jdbc.Driver");
    dataSource.setUrl(getUrl());
    dataSource.setUsername(STANDARD_USERNAME);
    dataSource.setPassword(STANDARD_PASSWORD);

    Properties props = new Properties();
    props.setProperty(PropertyDefinition.LOGGER_LEVEL.name, "ALL");
    dataSource.setConnectionProperties(props);

    return dataSource;
  }
}
