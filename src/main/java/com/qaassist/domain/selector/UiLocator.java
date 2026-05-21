// src/main/java/com/qaassist/domain/selector/UiLocator.java
package com.qaassist.domain.selector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

/**
 * Умный локатор с метаданными для устойчивости и отладки.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UiLocator(
    UUID id,
    @NotBlank String value,              // Сам селектор: "[data-testid='login-btn']"
    LocatorType type,                    // CSS, XPATH, TEST_ID, ROLE, TEXT
    String description,                  // Человекочитаемое описание
    List<String> alternatives,           // Запасные варианты для fallback
    StabilityScore stability,            // Оценка устойчивости 0..100
    SelectorContext context,             // Метаданные: страница, фрейм, состояни
    boolean isPreferred,                 // Помечен как основной для этого элемента
    String sourceRef                     // Figma/Jira/код, откуда взят
) {
  public enum LocatorType {
    TEST_ID("data-testid", 100),
    ROLE("role/aria", 90),
    CSS_ATTRIBUTE("[attr]", 80),
    CSS_CLASS(".class", 70),
    CSS_TAG("tag", 60),
    XPATH("//...", 40),
    TEXT("text=", 30);

    private final String example;
    private final int priority; // Чем выше — тем предпочтительнее

    LocatorType(String example, int priority) {
      this.example = example;
      this.priority = priority;
    }

    public static LocatorType fromValue(String value) {
      if (value == null) return CSS_CLASS;
      return switch (value.toLowerCase()) {
        case "testid", "data-testid" -> TEST_ID;
        case "role", "aria" -> ROLE;
        case "xpath" -> XPATH;
        case "text" -> TEXT;
        default -> CSS_CLASS;
      };
    }
  }

  public record StabilityScore(int value, List<String> factors) {
    public boolean isStable() { return value >= 80; }
    public static StabilityScore unstable(String reason) {
      return new StabilityScore(30, List.of(reason));
    }
    public static StabilityScore stable(String factor) {
      return new StabilityScore(95, List.of(factor));
    }
  }

  public record SelectorContext(
      String pageUrl,
      String frameSelector,
      List<String> requiredStates,  // e.g. ["logged-in", "mobile-view"]
      String viewport              // e.g. "1920x1080"
  ) {}

  // Utility: сравнение по приоритету типа
  public int priorityScore() {
    return type.priority + (isPreferred ? 20 : 0) + (stability.isStable() ? 15 : 0);
  }
}