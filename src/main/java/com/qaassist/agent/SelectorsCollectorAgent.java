// src/main/java/com/qaassist/agent/SelectorsCollectorAgent.java
package com.qaassist.agent;

import com.qaassist.domain.artifact.TestCase;
import com.qaassist.domain.artifact.TestStep;
import com.qaassist.domain.artifact.TestSuite;
import com.qaassist.domain.selector.SelectorCatalog;
import com.qaassist.domain.selector.UiLocator;
import com.qaassist.integration.playwright.PlaywrightInspector;
import com.qaassist.integration.playwright.SelectorOptimizer;
import com.qaassist.service.SelectorCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelectorsCollectorAgent {

  private final PlaywrightInspector inspector;
  private final SelectorOptimizer optimizer;
  private final SelectorCacheService cacheService;

  /**
   * Собирает и оптимизирует селекторы для UI-шагов тестового сьюта.
   */
  public TestSuite collectAndEnrich(TestSuite suite, String projectId, String baseUrl) {
    log.info("🎯 Collecting selectors for {} test cases", suite.testCases().size());

    // 1. Извлекаем целевые элементы из UI-шагов
    List<String> targetElements = extractTargetElements(suite);
    if (targetElements.isEmpty()) {
      log.debug("⏭️ No UI steps found, skipping selector collection");
      return suite;
    }

    // 2. Инспектируем страницу через Playwright
    List<UiLocator> rawLocators = inspector.inspectPage(baseUrl, targetElements);

    // 3. Оптимизируем каждый набор кандидатов
    Map<String, UiLocator> optimized = rawLocators.stream()
        .collect(Collectors.toMap(
            loc -> loc.description().replace("Locator for: ", ""),
            loc -> optimizer.optimize(List.of(loc),
                loc.description().replace("Locator for: ", "")),
            (a, b) -> a.priorityScore() > b.priorityScore() ? a : b
        ));

    // 4. Обновляем кэш и получаем актуальный каталог
    SelectorCatalog updatedCatalog = cacheService.mergeAndSave(projectId, new ArrayList<>(optimized.values()));

    // 5. Обогащаем тестовые шаги лучшими селекторами
    TestSuite enriched = enrichSuiteWithSelectors(suite, optimized, updatedCatalog);

    log.info("✅ Selector collection complete: {} locators cached, {} steps enriched",
        updatedCatalog.metadata().totalLocators(), countEnrichedSteps(enriched));
    return enriched;
  }

  private List<String> extractTargetElements(TestSuite suite) {
    return suite.testCases().stream()
        .flatMap(tc -> tc.steps().stream())
        .filter(TestStep::isUiStep)
        .map(step -> step.uiLocator().map(UiLocator::description).orElse(""))
        .filter(desc -> !desc.isBlank() && desc.startsWith("Locator for: "))
        .map(desc -> desc.replace("Locator for: ", "").trim())
        .distinct()
        .toList();
  }

  private TestSuite enrichSuiteWithSelectors(TestSuite suite,
      Map<String, UiLocator> optimized,
      SelectorCatalog catalog) {
    List<TestCase> enrichedCases = suite.testCases().stream()
        .map(tc -> {
          List<TestStep> enrichedSteps = tc.steps().stream()
              .map(step -> {
                if (!step.isUiStep()) return step;

                String elementKey = step.uiLocator().map(UiLocator::description)
                    .map(d -> d.replace("Locator for: ", "").trim())
                    .orElse(null);

                if (elementKey != null && optimized.containsKey(elementKey)) {
                  UiLocator best = optimized.get(elementKey);
                  return new TestStep(
                      step.id(), step.action(), step.expected(),
                      step.estimatedDurationSeconds(), step.assertions(),
                      step.testDataRef(),
                      java.util.Optional.of(new TestStep.UiLocator(
                          best.value(), best.type(),
                          step.uiLocator().flatMap(UiLocator::frame),
                          best.description()
                      )),
                      step.apiCall()
                  );
                }
                return step;
              })
              .toList();
          return new TestCase(
              tc.id(), tc.name(), tc.description(), tc.priority(), tc.type(),
              enrichedSteps, tc.traceability(), tc.createdAt(), tc.updatedAt(), tc.linkedRequirements()
          );
        })
        .toList();

    return new TestSuite(
        suite.id(), suite.suiteName(), suite.sourceStoryId(),
        enrichedCases, suite.generatedAt(), suite.metadata()
    );
  }

  private long countEnrichedSteps(TestSuite suite) {
    return suite.testCases().stream()
        .flatMap(tc -> tc.steps().stream())
        .filter(TestStep::isUiStep)
        .filter(step -> step.uiLocator().isPresent())
        .count();
  }
}