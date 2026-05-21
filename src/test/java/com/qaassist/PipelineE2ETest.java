// src/test/java/com/qaassist/PipelineE2ETest.java
package com.qaassist;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.qaassist.api.PipelineController.ExecuteRequest;
import com.qaassist.pipeline.PipelineExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@WireMockTest(httpPort = 9999)
@ActiveProfiles("test-e2e")
class PipelineE2ETest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  @DisplayName("E2E: Полный пайплайн от Jira до MR")
  void fullPipelineHappyPath() {
    // 1. Моки внешних сервисов
    stubFor(get(urlPathMatching("/rest/api/3/issue/.*"))
        .willReturn(aResponse().withStatus(200).withBody(mockJiraIssue())));
    stubFor(post(urlPathEqualTo("/testcases"))
        .willReturn(aResponse().withStatus(201).withBody("{\"id\":\"1\"}")));
    stubFor(post(urlPathMatching("/api/v4/projects/.*"))
        .willReturn(aResponse().withStatus(201).withBody("{\"web_url\":\"https://mr.test/1\",\"iid\":\"1\"}")));

    // 2. Запуск
    ResponseEntity<Map> startResp = restTemplate.postForEntity(
        "/api/v1/pipeline/execute",
        new ExecuteRequest("QA-E2E", "PROJ-123"),
        Map.class
    );
    assertThat(startResp.getStatusCode().is2xxSuccessful()).isTrue();
    String pipelineId = (String) startResp.getBody().get("pipelineId");

    // 3. Ожидание завершения
    await().atMost(java.time.Duration.ofSeconds(30)).untilAsserted(() -> {
      ResponseEntity<PipelineExecution> status = restTemplate.getForEntity(
          "/api/v1/pipeline/" + pipelineId, PipelineExecution.class
      );
      assertThat(status.getBody().getStatus()).isIn(
          PipelineExecution.PipelineStatus.COMPLETED,
          PipelineExecution.PipelineStatus.PARTIALLY_COMPLETED
      );
    });

    // 4. Верификация
    PipelineExecution result = restTemplate.getForObject(
        "/api/v1/pipeline/" + pipelineId, PipelineExecution.class
    );
    assertThat(result).isNotNull();
    assertThat(result.getMrResult()).isNotNull();
    assertThat(result.getMrResult().success()).isTrue();
  }

  private String mockJiraIssue() { return """
        {"key":"PROJ-123","fields":{"summary":"E2E Test","description":"Full flow","status":{"name":"Open"},"priority":{"name":"High"},"reporter":{"displayName":"QA"},"labels":[],"attachment":[],"created":"2024-01-01T00:00:00Z","updated":"2024-01-01T00:00:00Z"}}
        """; }
}