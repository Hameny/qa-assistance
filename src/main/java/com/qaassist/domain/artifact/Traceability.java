// src/main/java/com/qaassist/domain/artifact/Traceability.java
package com.qaassist.domain.artifact;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

/**
 * Обеспечивает трассируемость: тест → требования → критерии приёмки.
 * Ключевой элемент для отчётности и покрытия.
 */
public record Traceability(
    UUID id,
    @NotEmpty List<String> requirementIds,      // IDs из Jira/AD
    @NotEmpty List<String> acceptanceCriteria,  // тексты или ссылки
    String userStory,                           // опционально: полная история
    String sourceArtifactRef                    // ссылка на исходный документ
) implements TestArtifact {

  // Переопределяем методы из TestArtifact для смысловой согласованности
  @Override
  public UUID id() { return id; }

  @Override
  public String name() {
    return "Traceability:" + String.join(",", requirementIds);
  }

  @Override
  public com.qaassist.domain.common.Priority priority() {
    return com.qaassist.domain.common.Priority.HIGH; // всегда важно
  }

  @Override
  public java.time.Instant createdAt() { return java.time.Instant.now(); }

  @Override
  public java.time.Instant updatedAt() { return java.time.Instant.now(); }

  @Override
  public List<com.qaassist.domain.requirement.Requirement> linkedRequirements() {
    return List.of(); // Traceability — мета-артефакт, не ссылается на другие требования
  }

  // Utility: проверка полного покрытия
  public boolean coversAll(List<String> expectedRequirementIds) {
    return requirementIds.containsAll(expectedRequirementIds);
  }

  // Utility: генерация отчёта для Quality Gate
  public TraceabilityReport generateReport() {
    return new TraceabilityReport(
        requirementIds.size(),
        acceptanceCriteria.size(),
        requirementIds.stream().distinct().count()
    );
  }

  public record TraceabilityReport(
      int totalRequirements,
      int acceptanceCriteriaCount,
      long uniqueRequirements
  ) {
    public double coveragePercent(int totalExpected) {
      return totalExpected > 0
          ? (uniqueRequirements * 100.0 / totalExpected)
          : 100.0;
    }
  }
}