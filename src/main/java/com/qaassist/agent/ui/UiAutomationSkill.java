// src/main/java/com/qaassist/agent/ui/UiAutomationSkill.java
package com.qaassist.agent.ui;

import com.qaassist.agent.ui.engine.PlaywrightTemplateEngine;
import com.qaassist.agent.ui.model.UiTestStructure;
import com.qaassist.agent.ui.model.UiTestStructure.*;
import com.qaassist.data.TestDataResolver;
import com.qaassist.domain.artifact.TestCase;
import com.qaassist.domain.artifact.TestStep;
import com.qaassist.domain.artifact.TestSuite;
import com.qaassist.domain.selector.SelectorCatalog;
import com.qaassist.domain.selector.UiLocator;
import com.qaassist.service.SelectorCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UiAutomationSkill {

  private final PlaywrightTemplateEngine templateEngine;
  private final TestDataResolver dataResolver;
  private final SelectorCacheService selectorCache;

  /**
   * Преобразует TestSuite в Page Object + Playwright тест.
   */
  public PlaywrightTemplateEngine.GeneratedUiTest generate(TestSuite suite, String projectId, String baseUrl) {
    log.info("🖥️ Generating UI tests for suite: {}", suite.suiteName());

    // 1. Загружаем лучшие селекторы из кэша
    SelectorCatalog catalog = selectorCache.loadCatalog(projectId);

    // 2. Маппинг в структуру генерации
    UiTestStructure structure = mapToStructure(suite, projectId, baseUrl, catalog);

    // 3. Генерация кода
    PlaywrightTemplateEngine.GeneratedUiTest generated = templateEngine.generate(structure);

    // 4. Базовая валидация
    validateGeneratedCode(generated);

    log.info("✅ Generated UI test: {} + {} methods",
        generated.pageClassName(), structure.testMethods().size());
    return generated;
  }

  private UiTestStructure mapToStructure(TestSuite suite, String projectId, String baseUrl, SelectorCatalog catalog) {
    String packageName = "com.qaassist.tests.ui." + projectId.toLowerCase().replace("-", ".");
    String pageName = suite.suiteName().replaceAll("[^a-zA-Z0-9]", "") + "Page";
    String testName = suite.suiteName().replaceAll("[^a-zA-Z0-9]", "") + "Test";

    List<PageField> fields = extractPageFields(suite, catalog);
    List<UiTestMethod> methods = suite.testCases().stream()
        .filter(tc -> tc.steps().stream().anyMatch(TestStep::isUiStep))
        .map(tc -> mapToMethod(tc, projectId, fields))
        .toList();

    return new UiTestStructure(
        packageName, pageName, testName, baseUrl,
        fields, methods.isEmpty() ? List.of(createDummyMethod()) : methods,
        Map.of("org.junit.jupiter.api.*", "org.junit.jupiter.api.*",
            "com.microsoft.playwright.*", "com.microsoft.playwright.*")
    );
  }

  private List<PageField> extractPageFields(TestSuite suite, SelectorCatalog catalog) {
    return suite.testCases().stream()
        .flatMap(tc -> tc.steps().stream())
        .filter(TestStep::isUiStep)
        .flatMap(step -> step.uiLocator().stream())
        .map(loc -> {
          UiLocator best = catalog.findBest(loc.description().replace("Locator for: ", "").trim());
          return new PageField(
              sanitizeFieldName(loc.description()),
              best != null ? best.type().name().toLowerCase() : "locator",
              best != null ? best.value() : loc.selector(),
              detectFieldType(loc)
          );
        })
        .distinct()
        .toList();
  }

  private UiTestMethod mapToMethod(TestCase tc, String projectId, List<PageField> fields) {
    List<StepAction> actions = tc.steps().stream()
        .filter(TestStep::isUiStep)
        .map(step -> {
          String target = step.uiLocator().map(l -> sanitizeFieldName(l.description())).orElse("unknown");
          String resolvedInput = step.testDataRef().map(ref -> dataResolver.resolve(ref, projectId)).orElse("");

          return new StepAction(
              detectActionType(step),
              target,
              resolvedInput,
              step.assertions().stream().findFirst().map(a -> a.expectedValue()).orElse("")
          );
        })
        .toList();

    return new UiTestMethod(
        tc.name().replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase(),
        tc.description(),
        actions,
        List.of(tc.type().name().toLowerCase()),
        tc.priority().weight()
    );
  }

  private String sanitizeFieldName(String desc) {
    String cleaned = desc.replace("Locator for: ", "").trim()
        .replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
    return cleaned.isEmpty() ? "element" : cleaned;
  }

  private String detectActionType(TestStep step) {
    if (step.assertions().stream().anyMatch(a -> a.type() == Assertion.AssertionType.IS_PRESENT || a.type() == Assertion.AssertionType.EQUALS)) {
      return "ASSERT_VISIBLE";
    }
    if (step.testDataRef().isPresent()) return "FILL";
    return "CLICK";
  }

  private String detectFieldType(UiLocator loc) {
    String sel = loc.value().toLowerCase();
    if (sel.contains("input") || sel.contains("text")) return "INPUT";
    if (sel.contains("button") || sel.contains("btn") || sel.contains("click")) return "BUTTON";
    return "CONTAINER";
  }

  private UiTestMethod createDummyMethod() {
    return new UiTestMethod("placeholderTest", "Generated placeholder",
        List.of(new StepAction("ASSERT_VISIBLE", "root", "", "Page loaded")),
        List.of("ui"), 99);
  }

  private void validateGeneratedCode(PlaywrightTemplateEngine.GeneratedUiTest generated) {
    if (!generated.pageSource().contains("package ") || !generated.testSource().contains("@Test")) {
      log.warn("⚠️ Generated UI code missing critical structure");
    }
  }
}