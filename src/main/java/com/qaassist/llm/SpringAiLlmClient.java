package com.qaassist.llm;

import com.qaassist.config.properties.AppProperties;
import com.qaassist.util.JsonResponseExtractor;
import com.qaassist.util.JsonSchemaValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class SpringAiLlmClient implements LlmClientService {

  private final ChatClient chatClient;
  private final AppProperties.LlmProperties llmProps;
  private final ObjectMapper objectMapper;
  private final AtomicReference<PromptTrace> lastTrace = new AtomicReference<>();

  public SpringAiLlmClient(ChatModel chatModel, AppProperties properties, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.llmProps = properties.getLlm();
    this.chatClient = ChatClient.builder(chatModel).build();
  }

  @Override
  @Retryable(
      retryFor = {LlmResponseException.class, RuntimeException.class},
      maxAttemptsExpression = "#{@appProperties.llm.retry.maxAttempts}",
      backoff = @Backoff(delayExpression = "#{@appProperties.llm.retry.backoffMs}", multiplier = 2)
  )
  public String chat(String systemPrompt, String userPrompt, Map<String, Object> contextVars) {
    var traceId = UUID.randomUUID();
    var start = Instant.now();

    try {
      String resolvedPrompt = resolveVariables(userPrompt, contextVars);

      var response = chatClient.prompt()
          .system(systemPrompt)
          .user(resolvedPrompt)
          .call()
          .content();

      var latency = Duration.between(start, Instant.now());
      lastTrace.set(PromptTrace.success(traceId, systemPrompt, resolvedPrompt, contextVars, response, latency));

      log.debug("LLM Request [{}] completed in {}ms", traceId, latency.toMillis());
      return response;

    } catch (Exception e) {
      var latency = Duration.between(start, Instant.now());
      lastTrace.set(PromptTrace.failure(traceId, systemPrompt, userPrompt, contextVars, e, latency));

      log.error("LLM Request [{}] failed after {}ms: {}", traceId, latency.toMillis(), e.getMessage());
      throw new LlmResponseException("LLM call failed", e);
    }
  }

  @Override
  public <T> T chatAsJson(String systemPrompt, String userPrompt, Class<T> responseType) {
    String rawResponse = chat(systemPrompt, userPrompt, Map.of());

    String jsonBlock = JsonResponseExtractor.extractJson(rawResponse);
    if (jsonBlock == null) {
      throw new LlmResponseException("No valid JSON found in LLM response");
    }

    try {
      return objectMapper.readValue(jsonBlock, responseType);
    } catch (Exception e) {
      throw new LlmResponseException("Failed to deserialize LLM JSON response", e);
    }
  }

  @Override
  public PromptTrace getLastTrace() { return lastTrace.get(); }

  @Override
  public void clearTrace() { lastTrace.set(null); }

  private String resolveVariables(String prompt, Map<String, Object> vars) {
    if (vars == null || vars.isEmpty()) return prompt;

    String resolved = prompt;
    for (var entry : vars.entrySet()) {
      resolved = resolved.replace("%" + entry.getKey() + "%",
          entry.getValue() != null ? entry.getValue().toString() : "");
    }
    return resolved;
  }
}