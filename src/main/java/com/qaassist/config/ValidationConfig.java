package com.qaassist.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaassist.llm.LlmClientService;
import com.qaassist.validation.schema.JsonSchemaValidator;
import com.qaassist.validation.schema.LlmJsonParserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация слоя валидации JSON-ответов LLM.
 * Решает проблему циклической зависимости между LlmClientService и LlmJsonParserService.
 */
@Configuration
public class ValidationConfig {

  @Bean
  public LlmJsonParserService llmJsonParserService(
      JsonSchemaValidator validator,
      ObjectMapper objectMapper,
      LlmClientService llmClientService) {

    // Инициализация статического холдера (временное решение для обхода циклической зависимости)
    LlmJsonParserService.LlmClientServiceHolder.setClient(llmClientService);

    return new LlmJsonParserService(validator, objectMapper);
  }
}