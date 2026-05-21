// src/test/java/com/qaassist/agent/mr/MrCreatorAgentTest.java
package com.qaassist.agent.mr;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.qaassist.agent.mr.model.MrCreationResult;
import com.qaassist.domain.artifact.*;
import com.qaassist.domain.common.Priority;
import com.qaassist.domain.common.TestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@WireMockTest(httpPort = 8088)
class MrCreatorAgentTest {

  @Autowired private MrCreatorAgent mrCreator;

  @DynamicPropertySource
  static void configureGitlab(DynamicPropertyRegistry registry) {
    registry.add("qa.assist.gitlab.base-url", () -> "http://localhost:8088");
    registry.add("qa.assist.gitlab.token", () -> "test-token");
    registry.add("qa.assist.gitlab.project-id", () -> "123");
    registry.add("qa.assist.gitlab.default-branch", () -> "main");
  }

  @Test
  @DisplayName("Успешное создание MR: ветка → коммит → MR")
  void successfullyCreatesMergeRequest() {
    // Stubs
    stubFor(get(urlPathEqualTo("/api/v4/projects/123/repository/branches/qa-assist*"))
        .willReturn(aResponse().withStatus(404))); // ветка не существует

    stubFor(post(urlPathEqualTo("/api/v4/projects/123/repository/branches"))
        .willReturn(aResponse().withStatus(201)));

    stubFor(post(urlPathEqualTo("/api/v4/projects/123/repository/commits"))
        .willReturn(aResponse().withStatus(201)));

    stubFor(post(urlPathEqualTo("/api/v4/projects/123/merge_requests"))
        .willReturn(aResponse()
            .withStatus(201)
            .withHeader("Content-Type", "application/json")
            .withBody("""
                    {"web_url":"https://gitlab.com/proj/-/merge_requests/42","iid":"42","state":"opened"}
                    """)));

    TestSuite suite = createMockSuite();

    // Act
    MrCreationResult result = mrCreator.create(suite, List.of(), List.of());

    // Assert
    assertThat(result.success()).isTrue();
    assertThat(result.branchName()).startsWith("qa-assist/auto-tests-");
    assertThat(result.mrUrl()).contains("/merge_requests/42");
    assertThat(result.filesCommitted()).isGreaterThan(0);
  }

  private TestSuite createMockSuite() {
    var tc = new TestCase(UUID.randomUUID(), "Mock Test", "Auto-generated", Priority.MEDIUM, TestType.FUNCTIONAL,
        List.of(), null, Instant.now(), Instant.now(), List.of());
    return new TestSuite(UUID.randomUUID(), "MrTestSuite", "story-99", List.of(tc), Instant.now(),
        new TestSuite.GenerationMetadata(List.of("BOUNDARY"), List.of()));
  }
}