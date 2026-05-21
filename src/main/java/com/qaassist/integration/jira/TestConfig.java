// src/test/java/com/qaassist/integration/jira/TestConfig.java
package com.qaassist.integration.jira;

import com.qaassist.config.properties.AppProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {

  @Bean
  public AppProperties testAppProperties() {
    var props = new AppProperties();
    props.getJira().setBaseUrl("http://localhost:8089");
    props.getJira().setParsedFields(List.of("summary", "description", "status", "priority", "attachment"));
    return props;
  }
}