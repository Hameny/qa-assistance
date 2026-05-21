// src/main/java/com/qaassist/agent/zephyr/ZephyrApiClient.java
package com.qaassist.agent.zephyr;

import com.qaassist.config.properties.ZephyrProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZephyrApiClient {

  private final WebClient zephyrWebClient;
  private final ZephyrProperties props;
  private final ObjectMapper objectMapper;

  /**
   * Создаёт или обновляет тест-кейс в Zephyr Scale.
   * Идемпотентность обеспечивается через externalId.
   */
  @Retryable(
      retryFor = {WebClientResponseException.TooManyRequests.class, WebClientResponseException.InternalServerError.class},
      maxAttemptsExpression = "#{@zephyrProperties.retryMaxAttempts}",
      backoff = @Backoff(delayExpression = "#{@zephyrProperties.retryBackoffMs}", multiplier = 2)
  )
  public Mono<ZephyrResponse> createOrUpdateTestCase(String externalId, Map<String, Object> payload) {
    return zephyrWebClient.post()
        .uri("/testcases")
        .bodyValue(payload)
        .retrieve()
        .onStatus(
            status -> status.value() == 409, // Conflict: уже существует
            resp -> Mono.error(new ZephyrConflictException("TestCase with externalId " + externalId + " already exists"))
        )
        .bodyToMono(JsonNode.class)
        .map(node -> new ZephyrResponse(
            node.path("key").asText(),
            node.path("status").asText(),
            node.path("id").asText()
        ))
        .doOnSuccess(r -> log.debug("✅ Zephyr API success: {}", r))
        .doOnError(e -> log.warn("❌ Zephyr API error for {}: {}", externalId, e.getMessage()));
  }

  /**
   * Проверяет существование кейса по externalId (для skipExisting логики)
   */
  public Mono<Boolean> existsByExternalId(String externalId) {
    return zephyrWebClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/testcases")
            .queryParam("projectId", props.getProjectKey())
            .queryParam("externalId", externalId)
            .build())
        .retrieve()
        .bodyToMono(JsonNode.class)
        .map(node -> node.path("values").size() > 0)
        .onErrorReturn(false);
  }

  public record ZephyrResponse(String key, String status, String id) {}
  public static class ZephyrConflictException extends RuntimeException {
    public ZephyrConflictException(String message) { super(message); }
  }
}