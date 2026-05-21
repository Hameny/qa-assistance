package com.qaassist.domain.context;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "context_entry")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContextEntryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "project_id", nullable = false)
  private String projectId;

  @Column(nullable = false)
  private String category;

  @Column(name = "context_key", nullable = false)
  private String contextKey;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  @Column(name = "content_hash")
  private String contentHash;

  @Column(nullable = false)
  private Integer version;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;
}