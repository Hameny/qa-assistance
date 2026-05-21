package com.qaassist.llm;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PromptTrace(
    UUID id,
    String systemPrompt,
    String userPrompt,
    Map<String, Object> contextVars,
    String rawResponse,
    Duration latency,
    Instant timestamp,
    Exception error
) {
  public static PromptTrace success(UUID id, String sys, String usr, Map<String, Object> vars,
      String response, Duration latency) {
    return new PromptTrace(id, sys, usr, vars, response, latency, Instant.now(), null);
  }

  public static PromptTrace failure(UUID id, String sys, String usr, Map<String, Object> vars,
      Exception error, Duration latency) {
    return new PromptTrace(id, sys, usr, vars, null, latency, Instant.now(), error);
  }

  public boolean isSuccess() { return error == null; }
  public String getErrorMessage() { return error != null ? error.getMessage() : null; }
}