package com.qaassist.domain.context;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContextEntryRepository extends JpaRepository<ContextEntryEntity, UUID> {

  List<ContextEntryEntity> findByProjectIdAndCategoryIn(String projectId, List<String> categories);

  Optional<ContextEntryEntity> findByProjectIdAndCategoryAndContextKey(String projectId, String category, String contextKey);

  @Query("SELECT e FROM ContextEntryEntity e WHERE e.projectId = :projectId AND e.category IN :categories")
  List<ContextEntryEntity> findByProjectAndCategories(String projectId, List<String> categories);

  @Modifying
  @Query("DELETE FROM ContextEntryEntity e WHERE e.projectId = :projectId AND e.category = :category")
  void deleteByProjectIdAndCategory(String projectId, String category);
}