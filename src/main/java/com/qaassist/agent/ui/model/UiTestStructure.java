// src/main/java/com/qaassist/agent/ui/model/UiTestStructure.java
package com.qaassist.agent.ui.model;

import java.util.List;
import java.util.Map;

/**
 * Промежуточная структура для генерации Playwright Java кода.
 * Отделяет бизнес-логику сценариев от синтаксиса фреймворка.
 */
public record UiTestStructure(
    String packageName,
    String pageClassName,
    String testClassName,
    String baseUrl,
    List<PageField> pageFields,
    List<UiTestMethod> testMethods,
    Map<String, String> imports
) {
  public record PageField(
      String fieldName,
      String locatorStrategy, // getbyTestId, getByRole, locator
      String locatorValue,
      String fieldType        // INPUT, BUTTON, TEXT, CONTAINER
  ) {}

  public record UiTestMethod(
      String methodName,
      String description,
      List<StepAction> actions,
      List<String> tags,
      int priority
  ) {}

  public record StepAction(
      String type,           // CLICK, FILL, ASSERT_VISIBLE, ASSERT_TEXT, NAVIGATE
      String targetField,    // Имя поля из PageField или селектор
      String inputValue,     // Данные для ввода (могут содержать {{PLACEHOLDERS}})
      String assertion       // Ожидаемое состояние/текст
  ) {}
}