// src/main/java/com/qaassist/domain/common/ValidationResult.java
package com.qaassist.domain.common;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Универсальный объект результата валидации (JSON Schema, Quality Gates, Data Integrity).
 */
public record ValidationResult(
    boolean isValid,
    Set<String> errors,
    String warningMessage
) {
  public static ValidationResult success() {
    return new ValidationResult(true, Set.of(), null);
  }

  public static ValidationResult failure(String... errors) {
    return new ValidationResult(false, Set.of(errors), null);
  }

  public static ValidationResult failure(Set<String> errors) {
    return new ValidationResult(false, errors, null);
  }

  public static ValidationResult warning(String msg) {
    return new ValidationResult(true, Set.of(), msg);
  }

  public String formatErrors() {
    return errors.isEmpty() ? "No errors" : errors.stream().collect(Collectors.joining("\n- "));
  }

  public ValidationResult merge(ValidationResult other) {
    if (!other.isValid()) {
      return ValidationResult.failure(this.errors, other.errors);
    }
    if (this.warningMessage != null || other.warningMessage != null) {
      String combined = (this.warningMessage != null ? this.warningMessage : "") +
          (other.warningMessage != null ? " | " + other.warningMessage : "");
      return ValidationResult.warning(combined);
    }
    return this;
  }

  private static ValidationResult failure(Set<String> a, Set<String> b) {
    var merged = new java.util.HashSet<>(a);
    merged.addAll(b);
    return ValidationResult.failure(merged);
  }
}