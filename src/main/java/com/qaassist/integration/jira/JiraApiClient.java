// src/main/java/com/qaassist/integration/jira/JiraApiClient.java
package com.qaassist.integration.jira;

import com.qaassist.config.properties.AppProperties;
import com.qaassist.domain.external.JiraIssue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JiraApiClient {

  private final WebClient jiraWebClient;
  private final AppProperties.JiraProperties jiraProps;

  private static final String API_PATH = "/rest/api/3/issue";

  /**
   * Загружает задачу по ключу (PROJ-123).
   */
  @Retryable(
      retryFor = {WebClientResponseException.ServiceUnavailable.class, WebClientResponseException.GatewayTimeout.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2)
  )
  public Mono<JiraIssue> fetchIssue(String issueKey) {
    log.debug("📡 Fetching Jira issue: {}", issueKey);

    return jiraWebClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(API_PATH + "/{key}")
            .queryParam("fields", String.join(",", jiraProps.getParsedFields()))
            .queryParam("expand", "attachment,renderedFields")
            .build(issueKey))
        .retrieve()
        .bodyToMono(JiraResponseWrapper.class)
        .map(JiraResponseWrapper::toDomain)
        .doOnNext(issue -> log.debug("✅ Fetched: {} - {}", issue.key(), issue.summary()))
        .doOnError(e -> log.error("❌ Failed to fetch {}: {}", issueKey, e.getMessage()));
  }

  /**
   * Скачивает содержимое вложения по URL.
   */
  @Retryable(
      retryFor = {WebClientResponseException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 500)
  )
  public Mono<byte[]> downloadAttachment(String contentUrl) {
    log.debug("📥 Downloading attachment: {}", contentUrl);

    return jiraWebClient.get()
        .uri(contentUrl)
        .retrieve()
        .bodyToMono(byte[].class)
        .doOnError(e -> log.error("❌ Failed to download {}: {}", contentUrl, e.getMessage()));
  }

  /**
   * Вспомогательный рекорд для десериализации ответа Jira.
   */
  private record JiraResponseWrapper(
      String key,
      Fields fields
  ) {
    public JiraIssue toDomain() {
      return new JiraIssue(
          key,
          fields.summary,
          fields.description,
          fields.status.name,
          fields.priority.name,
          fields.assignee != null ? fields.assignee.displayName : null,
          fields.reporter.displayName,
          fields.labels,
          fields.attachment.stream().map(JiraAttachmentWrapper::toDomain).toList(),
          fields.customFields,
          fields.created,
          fields.updated,
          fields.epicLink,
          List.of() // issueLinks можно добавить при необходимости
      );
    }

    private record Fields(
        String summary,
        String description,
        Status status,
        Priority priority,
        User assignee,
        User reporter,
        List<String> labels,
        List<JiraAttachmentWrapper> attachment,
        Map<String, Object> customFields,
        Instant created,
        Instant updated,
        String epicLink
    ) {}

    private record Status(String name) {}
    private record Priority(String name) {}
    private record User(String displayName) {}
    private record JiraAttachmentWrapper(
        String id, String filename, String mimeType, long size,
        Content content, String author, Instant created
    ) {
      public JiraAttachment toDomain() {
        return new JiraAttachment(id, filename, mimeType, size, content.url, author, created);
      }
      private record Content(String url) {}
    }
  }
}