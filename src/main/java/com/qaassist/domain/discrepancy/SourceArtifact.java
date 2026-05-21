// src/main/java/com/qaassist/domain/discrepancy/SourceArtifact.java
package com.qaassist.domain.discrepancy;

public record SourceArtifact(
    SourceType type,
    String reference,      // Jira:PROJ-123, figma:file/xyz, openapi.yaml#path
    String snippet,        // Цитата из источника
    Instant lastUpdated
) {
  public enum SourceType {
    JIRA, CONFLUENCE, FIGMA, OPENAPI, TEST_SUITE, SOURCE_CODE, GLOBAL_CONTEXT
  }
}