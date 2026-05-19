// src/main/java/com/qaassist/domain/artifact/Assertion.java
package com.qaassist.domain.artifact;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * Проверка (assertion) внутри шага теста.
 * Поддерживает различные типы валидации.
 */
public record Assertion(
    UUID id,
    @NotBlank String description,
    AssertionType type,
    String expectedValue,
    String actualExpression,  // SpEL или JSONPath для извлечения значения
    boolean critical          // если false — warning, а не failure
) {
  public enum AssertionType {
    EQUALS,
    CONTAINS,
    MATCHES_REGEX,
    IS_PRESENT,
    IS_EMPTY,
    GREATER_THAN,
    LESS_THAN,
    JSON_SCHEMA_VALID,
    CUSTOM_SCRIPT  // для сложных проверок через Groovy/JS
  }

  // Factory methods для удобства
  public static Assertion equals(String desc, String expected, String actualExpr) {
    return new Assertion(UUID.randomUUID(), desc, AssertionType.EQUALS, expected, actualExpr, true);
  }

  public static Assertion isPresent(String desc, String actualExpr) {
    return new Assertion(UUID.randomUUID(), desc, AssertionType.IS_PRESENT, null, actualExpr, true);
  }

  public static Assertion warning(String desc, AssertionType type, String expected, String actualExpr) {
    return new Assertion(UUID.randomUUID(), desc, type, expected, actualExpr, false);
  }
}