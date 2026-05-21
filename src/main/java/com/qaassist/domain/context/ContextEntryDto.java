package com.qaassist.domain.context;

import java.util.UUID;
import java.time.Instant;

public record ContextEntryDto(
    UUID id,
    String projectId,
    String category,
    String contextKey,
    String content,
    int version,
    Instant updatedAt
) {
  public static ContextEntryDto fromEntity(ContextEntryEntity entity) {
    return new ContextEntryDto(
        entity.getId(),
        entity.getProjectId(),
        entity.getCategory(),
        entity.getContextKey(),
        entity.getContent(),
        entity.getVersion(),
        entity.getUpdatedAt()
    );
  }
}