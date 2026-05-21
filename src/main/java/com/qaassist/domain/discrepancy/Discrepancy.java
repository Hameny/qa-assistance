// src/main/java/com/qaassist/domain/discrepancy/Discrepancy.java
package com.qaassist.domain.discrepancy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Представление расхождения между артефактами: требования ↔ дизайн ↔ тесты ↔ код.
 */
public record Discrepancy(
    UUID id,
    String title,
    String description,
    DiscrepancyType type,
    Severity severity,
    List<SourceArtifact> sources,          // Где найдено противоречие
    SourceOfTruth recommendedSource,       // Какой источник считать истинным
    List<FixStrategy> possibleFixes,       // Стратегии авто-исправления
    ResolutionStatus status,
    Instant detectedAt,
    Instant resolvedAt,
    String resolutionNote
) {
  public boolean isAutoFixable() {
    return status == ResolutionStatus.DETECTED &&
        !possibleFixes.isEmpty() &&
        recommendedSource != null;
  }

  public enum DiscrepancyType {
    REQUIREMENT_CONTRADICTION,    // Два требования противоречат друг другу
    DESIGN_MISMATCH,              // Figma ≠ описание в Jira
    TEST_COVERAGE_GAP,            // Требование без тест-кейса
    SELECTOR_MISMATCH,            // CSS/XPath не соответствует макету
    API_CONTRACT_VIOLATION,       // Эндпоинт ≠ спецификация OpenAPI
    DATA_INCONSISTENCY            // Тестовые данные ≠ реальная схема БД
  }

  public enum Severity {
    CRITICAL,  // Блокирует релиз
    HIGH,      // Требует ручного вмешательства
    MEDIUM,    // Можно авто-фикснуть
    LOW        // Информационное, не блокирует
  }

  public enum ResolutionStatus {
    DETECTED,
    AUTO_FIXED,
    ESCALATED,      // Передано инженеру
    IGNORED,        // Помечено как "не баг"
    RESOLVED_MANUALLY
  }
}