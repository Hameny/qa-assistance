package com.qaassist.integration.jira;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.qaassist.config.properties.AppProperties;
import com.qaassist.domain.external.JiraIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;

@WebFluxTest(JiraApiClient.class)
@WireMockTest(httpPort = 8089)
@Import(TestConfig.class) // Конфиг с моком AppProperties
class JiraApiClientTest {

  @Autowired
  private JiraApiClient client;

  @Autowired
  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    // WireMock stubs будут настроены в тестах
  }

  @Test
  @DisplayName("Успешная загрузка задачи из Jira")
  void fetchesIssueSuccessfully() {
    stubFor(get(urlPathMatching("/rest/api/3/issue/PROJ-123.*"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("""
                    {
                      "key": "PROJ-123",
                      "fields": {
                        "summary": "Login feature",
                        "description": "Implement OAuth2 login",
                        "status": {"name": "In Progress"},
                        "priority": {"name": "HIGH"},
                        "reporter": {"displayName": "QA Bot"},
                        "labels": ["auth", "security"],
                        "attachment": [],
                        "created": "2024-01-15T10:00:00.000+0000",
                        "updated": "2024-01-16T14:30:00.000+0000"
                      }
                    }
                    """)));

    JiraIssue issue = client.fetchIssue("PROJ-123").block();

    assertThat(issue).isNotNull();
    assertThat(issue.key()).isEqualTo("PROJ-123");
    assertThat(issue.summary()).isEqualTo("Login feature");
    assertThat(issue.priority()).isEqualTo("HIGH");
    assertThat(issue.labels()).contains("auth", "security");
  }

  @Test
  @DisplayName("Обработка 404 ошибки")
  void handlesNotFound() {
    stubFor(get(urlPathMatching("/rest/api/3/issue/INVALID-999.*"))
        .willReturn(aResponse().withStatus(404)));

    JiraIssue result = client.fetchIssue("INVALID-999").block();

    assertThat(result).isNull(); // onErrorResume вернёт null или error-контекст
  }
}