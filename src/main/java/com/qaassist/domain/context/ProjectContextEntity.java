// src/main/java/com/qaassist/domain/context/ProjectContextEntity.java
package com.qaassist.domain.context;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_context", indexes = {
    @Index(name = "idx_ctx_lookup", columnList = "project_id, context_type, is_active")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectContextEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "project_id", nullable = false)
  private String projectId;

  @Enumerated(EnumType.STRING)
  @Column(name = "context_type", nullable = false)
  private ContextType type;

  private String sourceRef; // Ссылка на Confluence/Jira/файл
  private String title;

  @Column(columnDefinition = "TEXT")
  private String content;

  private String version;

  @Column(name = "is_active")
  private boolean isActive = true;

  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  public enum ContextType {
    ENDPOINTS, TEST_DATA, ARCHITECTURE, GLOSSARY, OPENAPI, BUSINESS_RULES, SECURITY_POLICIES
  }

  @PrePersist
  protected void onCreate() {
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}