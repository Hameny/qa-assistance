package com.qaassist.validation.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaassist.llm.LlmClientService;
import com.qaassist.util.JsonResponseExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class LlmJsonParserService {

  private final JsonSchemaValidator validator;
  private final ObjectMapper objectMapper;
  private static final int MAX_RECOVERY_ATTEMPTS = 2;

  public LlmJsonParserService(JsonSchemaValidator validator, ObjectMapper objectMapper) {
    this.validator = validator;
    this.objectMapper = objectMapper;
  }

  /**
   * Полная цепочка: извлечение JSON → валидация → десериализация → retry при ошибке.
   */
  public <T> T parseWithRecovery(
      String rawLlmResponse,
      String schemaId,
      Class<T> type,
      String systemPrompt,
      String originalUserPrompt
  ) {
    String json = JsonResponseExtractor.extractJson(rawLlmResponse);
    if (json == null) {
      throw new LlmParseException("No JSON structure found in LLM response");
    }

    ValidationResult result = validator.validate(schemaId, json);
    if (result.valid()) {
      return deserialize(json, type);
    }

    log.warn("⚠️ Schema validation failed. Attempting recovery...");
    return attemptRecovery(json, schemaId, type, systemPrompt, originalUserPrompt, result);
  }

  private <T> T attemptRecovery(
      String invalidJson, String schemaId, Class<T> type,
      String systemPrompt, String originalPrompt, ValidationResult lastError
  ) {
    String correctionPrompt = buildCorrectionPrompt(lastError.formatErrors(), originalPrompt);

    // Retry LLM call with explicit error context
    String retryResponse = LlmClientServiceHolder.getClient().chat(systemPrompt, correctionPrompt, Map.of());
    String retryJson = JsonResponseExtractor.extractJson(retryResponse);

    if (retryJson == null) {
      throw new LlmParseException("Recovery attempt returned no JSON");
    }

    ValidationResult retryResult = validator.validate(schemaId, retryJson);
    if (retryResult.valid()) {
      log.info("✅ Recovery successful. Valid JSON received.");
      return deserialize(retryJson, type);
    }

    throw new LlmParseException(
        "Failed to generate valid JSON after recovery. Errors: " + retryResult.formatErrors()
    );
  }

  private <T> T deserialize(String json, Class<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (Exception e) {
      throw new LlmParseException("Jackson deserialization failed: " + e.getMessage(), e);
    }
  }

  private String buildCorrectionPrompt(String errors, String originalPrompt) {
    return """
            ⚠️ VALIDATION ERROR DETECTED:
            
            Your previous response failed JSON Schema validation with these errors:
            ```
            %s
            ```
            
            ORIGINAL PROMPT:
            %s
            
            🛠️ INSTRUCTIONS:
            1. Fix ONLY the validation errors listed above.
            2. Return ONLY the corrected JSON object. NO markdown, NO explanations.
            3. Ensure all required fields are present and types match the schema.
            """.formatted(errors, originalPrompt);
  }

  // Вспомогательный holder для избежания циклических зависимостей при тестировании
  static class LlmClientServiceHolder {
    private static LlmClientService client;
    public static void setClient(LlmClientService c) { client = c; }
    public static LlmClientService getClient() { return client; }
  }
}