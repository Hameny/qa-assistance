package com.qaassist.agent;

import com.qaassist.agent.dto.LlmDecompositionResponse;
import com.qaassist.agent.mapper.DecompositionMapper;
import com.qaassist.agent.validator.DecompositionValidator;
import com.qaassist.domain.requirement.UserStory;
import com.qaassist.llm.LlmClientService;
import com.qaassist.prompt.PromptService;
import com.qaassist.prompt.model.RenderedPrompt;
import com.qaassist.validation.schema.LlmJsonParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementsDecomposerAgent {

  private final PromptService promptService;
  private final LlmClientService llmClient;
  private final LlmJsonParserService jsonParser;
  private final DecompositionMapper mapper;
  private final DecompositionValidator validator;

  /**
   * Полный цикл декомпозиции: промпт → LLM → валидация JSON → маппинг → бизнес-проверка.
   */
  public DecompositionResult decompose(String projectId, String rawRequirements, String contextSlice) {
    log.info("🚀 Starting decomposition for project: {}", projectId);

    // 1. Подготовка промпта
    RenderedPrompt rendered = promptService.preparePrompt(
        "requirements_decomposition",
        Map.of(
            "PROJECT", projectId,
            "REQUIREMENTS_TEXT", truncateIfTooLong(rawRequirements, 15000),
            "GLOBAL_CONTEXT_SLICE", contextSlice != null ? contextSlice : "Standard architecture"
        )
    );

    // 2. Вызов LLM
    String rawResponse = llmClient.chat(
        rendered.systemPrompt(),
        rendered.userPrompt(),
        Map.of()
    );

    // 3. Валидация JSON Schema + автоматический Recovery
    LlmDecompositionResponse llmDto = jsonParser.parseWithRecovery(
        rawResponse,
        "user-story",
        LlmDecompositionResponse.class,
        rendered.systemPrompt(),
        rendered.userPrompt()
    );

    // 4. Маппинг в доменную модель
    UserStory story = mapper.toDomain(llmDto, projectId);

    // 5. Бизнес-валидация
    List<String> warnings = validator.validate(story);

    log.info("✅ Decomposition completed. Title: {}, Warnings: {}", story.title(), warnings.size());
    return new DecompositionResult(story, llmDto.discrepancies(), warnings);
  }

  private String truncateIfTooLong(String text, int maxChars) {
    return text.length() > maxChars ? text.substring(0, maxChars) + "\n[TRUNCATED]" : text;
  }

  /**
   * Результат работы агента: доменная модель + найденные расхождения + предупреждения.
   */
  public record DecompositionResult(
      UserStory userStory,
      List<LlmDecompositionResponse.LlmDiscrepancy> discrepancies,
      List<String> validationWarnings
  ) {
    public boolean hasCriticalDiscrepancies() {
      return discrepancies.stream()
          .anyMatch(d -> d.severity().equalsIgnoreCase("HIGH") || d.severity().equalsIgnoreCase("CRITICAL"));
    }
  }
}