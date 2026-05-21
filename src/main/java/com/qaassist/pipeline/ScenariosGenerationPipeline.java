package com.qaassist.pipeline;

import com.qaassist.agent.ScenariosGeneratorAgent;
import com.qaassist.data.DataIntegrityValidator;
import com.qaassist.data.TestDataEnricher;
import com.qaassist.data.TestDataResolver;
import com.qaassist.domain.artifact.TestSuite;
import com.qaassist.domain.requirement.UserStory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Оркестратор пайплайна генерации сценариев.
 * Координирует: генерация → обогащение данными → валидация целостности.
 *
 * Это прекурсор к полному PipelineOrchestrator (Часть 20).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenariosGenerationPipeline {

  private final ScenariosGeneratorAgent generator;
  private final TestDataEnricher enricher;
  private final DataIntegrityValidator integrityValidator;
  private final TestDataResolver resolver;

  /**
   * Полный цикл: сырая User Story → валидный, обогащённый данными TestSuite.
   */
  public TestSuite generateAndEnrich(String projectId, UserStory story) {
    log.info("🚀 Starting scenario generation pipeline for story: {}", story.title());

    // 1. Генерация сценариев по техникам ISTQB (Часть 10)
    TestSuite rawSuite = generator.generate(story);
    log.debug("📋 Generated {} raw test cases", rawSuite.testCases().size());

    // 2. Обогащение тестовыми данными (Часть 12)
    TestSuite enrichedSuite = enricher.enrich(rawSuite, projectId);
    log.debug("💉 Enriched with dynamic/static fixtures");

    // 3. Валидация ссылочной целостности (проверка {{REF:...}})
    DataIntegrityValidator.ValidationResult integrity =
        integrityValidator.validate(enrichedSuite, resolver);

    if (!integrity.valid()) {
      log.error("❌ Data integrity check failed: {}", integrity.unresolvedRefs());
      resolver.clearRunContext(); // Очистка даже при ошибке
      throw new IllegalStateException(
          "Data integrity check failed. Unresolved refs: " + integrity.unresolvedRefs()
      );
    }

    // 4. Очистка потокового контекста (важно для ThreadLocal в TestDataResolver)
    resolver.clearRunContext();

    log.info("✅ Pipeline completed: {} test cases, {} total steps",
        enrichedSuite.testCases().size(), enrichedSuite.totalSteps());
    return enrichedSuite;
  }
}