package com.qaassist.agent;

import com.qaassist.domain.requirement.UserStory;
import com.qaassist.llm.LlmClientService;
import com.qaassist.prompt.PromptService;
import com.qaassist.prompt.model.RenderedPrompt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Агент декомпозиции требований.
 * Отвечает за преобразование сырых требований в структурированные User Stories.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementsDecomposerAgent {

  private final PromptService promptService;
  private final LlmClientService llmClient;

  /**
   * Запускает процесс декомпозиции.
   * @param projectId ID проекта для подстановки в промпт
   * @param rawRequirements Сырой текст требований из Jira/Confluence
   * @param contextSlice Срез Global Context (API, архитектура, глоссарий)
   * @return Структурированная User Story
   */
  public UserStory decompose(String projectId, String rawRequirements, String contextSlice) {
    log.info("🚀 Starting requirements decomposition for project: {}", projectId);

    // 1. Формируем валидированный промпт
    RenderedPrompt rendered = promptService.preparePrompt(
        "requirements_decomposition",
        Map.of(
            "PROJECT", projectId,
            "REQUIREMENTS_TEXT", rawRequirements,
            "GLOBAL_CONTEXT_SLICE", contextSlice != null ? contextSlice : "No additional context"
        )
    );

    // 2. Выполняем запрос к LLM
    String rawResponse = llmClient.chat(
        rendered.systemPrompt(),
        rendered.userPrompt(),
        Map.of()
    );

    log.debug("📥 LLM response length: {} chars", rawResponse.length());

    // 3. Парсинг ответа (валидация JSON Schema будет добавлена в Части 7)
    return parseAndValidateResponse(rawResponse);
  }

  /**
   * Заглушка для компиляции. Полная реализация с JsonSchemaValidator
   * и десериализацией будет в Части 10 (ScenariosGenerator).
   */
  private UserStory parseAndValidateResponse(String json) {
    // TODO:
    // 1. JsonResponseExtractor.extractJson(json)
    // 2. JsonSchemaValidator.validate(json, "user-story.schema.json")
    // 3. objectMapper.readValue(json, UserStory.class)
    throw new UnsupportedOperationException(
        "JSON parsing & Schema validation will be fully implemented in Part 7/10"
    );
  }
}