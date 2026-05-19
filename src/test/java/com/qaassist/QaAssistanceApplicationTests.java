// src/test/java/com/qaassist/QaAssistanceApplicationTests.java
package com.qaassist;

import com.qaassist.properties.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class QaAssistanceApplicationTests {

  @Autowired
  private AppProperties appProperties;

  @Test
  void contextLoads() {
    assertThat(appProperties).isNotNull();
    assertThat(appProperties.getLlm().getProvider()).isNotBlank();
  }

  @Test
  void qualityGatesConfigured() {
    var gates = appProperties.getPipeline().getQualityGates();
    assertThat(gates.isRequireJsonSchema()).isTrue();
    assertThat(gates.isRequireTraceability()).isTrue();
    assertThat(gates.getMinCoveragePercent()).isBetween(0, 100);
  }
}