// src/test/java/com/qaassist/agent/zephyr/ZephyrUploaderAgentTest.java
package com.qaassist.agent.zephyr;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.qaassist.agent.zephyr.model.ZephyrSyncReport;
import com.qaassist.domain.artifact.*;
import com.qaassist.domain.common.Priority;
import com.qaassist.domain.common.TestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@WireMockTest(httpPort = 8085)
class ZephyrUploaderAgentTest {

  @Autowired private ZephyrUploaderAgent uploader;

  @DynamicPropertySource
  static void configureZephyr(DynamicPropertyRegistry registry) {
    registry.add("qa.assist.zephyr.base-url", () -> "http://localhost:8085");
    registry.add("qa.assist.zephyr.token", () -> "test-token");
    registry.add("qa.assist.zephyr.project-key", () -> "QA-TEST");
    registry.add("qa.assist.zephyr.skip-existing", () -> true);
  }

  @Test
  @DisplayName("Успешная загрузка тест-кейсов в Zephyr")
  void successfullyUploadsTestCases() {
    // Stub: проверка существования
    stubFor(get(urlPathEqualTo("/testcases"))
        .withQueryParam("projectId", equalTo("QA-TEST"))
        .withQueryParam("externalId", equalTo("uuid-1"))
        .willReturn(aResponse().withStatus(200).withBody("{\"values\":[]}")));

    // Stub: создание
    stubFor(post(urlPathEqualTo("/testcases"))
        .willReturn(aResponse()
            .withStatus(201)
            .withHeader("Content-Type", "application/json")
            .withBody("""
                    {"key":"QA-TEST-100","status":"created","id":"12345"}
                    """)));

    TestSuite suite = createMockSuite();

    // Act
    ZephyrSyncReport report = uploader.upload(suite).block();

    // Assert
    assertThat(report).isNotNull();
    assertThat(report.totalProcessed()).isEqualTo(1);
    assertThat(report.created()).isEqualTo(1);
    assertThat(report.failed()).isZero();
    assertThat(report.isSuccessful()).isTrue();
  }

  @Test
  @DisplayName("Пропуск существующих кейсов (skipExisting=true)")
  void skipsExistingCases() {
    stubFor(get(urlPathEqualTo("/testcases"))
        .withQueryParam("projectId", equalTo("QA-TEST"))
        .withQueryParam("externalId", equalTo("uuid-1"))
        .willReturn(aResponse().withStatus(200).withBody("{\"values\":[{\"id\":\"exist\"}]}")));

    TestSuite suite = createMockSuite();
    ZephyrSyncReport report = uploader.upload(suite).block();

    assertThat(report.skipped()).isEqualTo(1);
    assertThat(report.created()).isZero();
    verify(0, postRequestedFor(urlPathEqualTo("/testcases")));
  }

  private TestSuite createMockSuite() {
    var step = new TestStep(UUID.randomUUID(), "Step 1", "Expected", 5,
        List.of(), java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty());
    var tc = new TestCase(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Test Case 1", "Desc",
        Priority.HIGH, TestType.FUNCTIONAL, List.of(step), null, Instant.now(), Instant.now(), List.of());
    return new TestSuite(UUID.randomUUID(), "SyncSuite", "story-1", List.of(tc), Instant.now(), new TestSuite.GenerationMetadata(List.of(), List.of()));
  }
}