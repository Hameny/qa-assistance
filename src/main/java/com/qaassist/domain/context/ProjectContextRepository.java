// src/main/java/com/qaassist/domain/context/ProjectContextRepository.java
package com.qaassist.domain.context;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectContextRepository extends JpaRepository<ProjectContextEntity, UUID> {

  List<ProjectContextEntity> findByProjectIdAndIsActiveTrue(String projectId);

  List<ProjectContextEntity> findByProjectIdAndTypeInAndIsActiveTrue(
      String projectId, List<ProjectContextEntity.ContextType> types);

  @Modifying
  @Transactional
  @Query("UPDATE ProjectContextEntity e SET e.isActive = false WHERE e.projectId = :projectId AND e.type = :type")
  int deactivateByProjectAndType(String projectId, ProjectContextEntity.ContextType type);
}