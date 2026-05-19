// src/main/java/com/qaassist/domain/artifact/TestArtifact.java
package com.qaassist.domain.artifact;

import com.qaassist.domain.common.Priority;
import com.qaassist.domain.requirement.Requirement;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Базовый контракт для всех тестируемых артефактов.
 * Использует sealed interface для контролируемой иерархии (Java 17).
 */
public sealed interface TestArtifact
    permits TestCase, TestSuite, TestStep, Assertion, Traceability {

  UUID id();
  String name();
  Priority priority();
  Instant createdAt();
  Instant updatedAt();

  /**
   * Связь с требованиями — ключевой элемент трассируемости.
   */
  List<Requirement> linkedRequirements();

  /**
   * Метаданные для аудита и отладки пайплайна.
   */
  default ArtifactMetadata metadata() {
    return new ArtifactMetadata(
        this.getClass().getSimpleName(),
        createdAt(),
        updatedAt(),
        List.of() // можно расширить: автор, версия, источник
    );
  }

  record ArtifactMetadata(
      String type,
      Instant createdAt,
      Instant updatedAt,
      List<String> tags
  ) {}
}