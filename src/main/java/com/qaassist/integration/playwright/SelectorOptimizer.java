// src/main/java/com/qaassist/integration/playwright/SelectorOptimizer.java
package com.qaassist.integration.playwright;

import com.qaassist.domain.selector.UiLocator;
import com.qaassist.domain.selector.UiLocator.LocatorType;
import com.qaassist.domain.selector.UiLocator.StabilityScore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SelectorOptimizer {

  private static final Pattern DYNAMIC_SUFFIX = Pattern.compile("-\\d+$|__\\d+$|_[a-f0-9]{8,}");

  /**
   * Оптимизирует список кандидатов: выбирает лучший, генерирует fallbacks, оценивает стабильность.
   */
  public UiLocator optimize(List<UiLocator> candidates, String elementKey) {
    if (candidates.isEmpty()) {
      return createFallbackLocator(elementKey);
    }

    // 1. Сортируем по приоритету
    UiLocator best = candidates.stream()
        .max(Comparator.comparingInt(UiLocator::priorityScore))
        .orElseThrow();

    // 2. Генерируем альтернативы из остальных кандидатов
    List<String> alternatives = candidates.stream()
        .filter(c -> !c.equals(best))
        .map(UiLocator::value)
        .limit(2)
        .toList();

    // 3. Пересчитываем стабильность с учётом контекста
    StabilityScore refined = assessStability(best, elementKey);

    // 4. Возвращаем оптимизированный локатор
    return new UiLocator(
        best.id(),
        best.value(),
        best.type(),
        best.description(),
        alternatives,
        refined,
        best.context(),
        best.isPreferred() || best.type() == LocatorType.TEST_ID,
        best.sourceRef()
    );
  }

  private StabilityScore assessStability(UiLocator locator, String elementKey) {
    List<String> factors = new ArrayList<>();
    int score = 50; // Базовый

    // +30 за data-testid
    if (locator.type() == LocatorType.TEST_ID) {
      score += 30;
      factors.add("Uses data-testid");
    }

    // +20 за ARIA role
    if (locator.type() == LocatorType.ROLE) {
      score += 20;
      factors.add("Uses semantic ARIA");
    }

    // -20 за динамические суффиксы
    if (DYNAMIC_SUFFIX.matcher(locator.value()).find()) {
      score -= 20;
      factors.add("Contains dynamic suffix");
    }

    // -15 за сложные XPath
    if (locator.type() == LocatorType.XPATH && locator.value().split("/").length > 5) {
      score -= 15;
      factors.add("Deep XPath hierarchy");
    }

    // +10 за наличие fallbacks
    if (locator.alternatives() != null && !locator.alternatives().isEmpty()) {
      score += 10;
      factors.add("Has fallback selectors");
    }

    return new StabilityScore(Math.min(100, Math.max(0, score)), factors);
  }

  private UiLocator createFallbackLocator(String elementKey) {
    return new UiLocator(
        UUID.randomUUID(),
        String.format("text=%s", elementKey),
        LocatorType.TEXT,
        "Fallback: text match for " + elementKey,
        List.of(),
        StabilityScore.unstable("No stable selector found"),
        new UiLocator.SelectorContext(null, null, List.of(), null),
        false,
        "fallback-generated"
    );
  }
}