package com.qaassist.service;

import com.qaassist.domain.context.*;
import com.qaassist.infra.context.FileContextLoader;
import com.qaassist.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalContextService {

  private final ContextEntryRepository repository;
  private final FileContextLoader fileLoader;
  private final AppProperties properties;

  /**
   * Загружает контекст из файлов, синхронизирует с БД и кэширует.
   * Вызывается при старте пайплайна или по расписанию.
   */
  @Transactional
  @CacheEvict(value = "global-context", key = "#projectId")
  public void syncContext(String projectId) {
    log.info("Syncing global context for project: {}", projectId);

    var fileEntries = fileLoader.loadFromFiles(projectId);
    if (fileEntries.isEmpty()) {
      log.info("No context files found for project {}", projectId);
      return;
    }

    // Удаляем старые записи той же категории
    var categories = fileEntries.stream()
        .map(ContextEntryDto::category)
        .distinct()
        .toList();

    for (String cat : categories) {
      repository.deleteByProjectIdAndCategory(projectId, cat);
    }

    // Сохраняем новые
    var entities = fileEntries.stream()
        .map(dto -> ContextEntryEntity.builder()
            .projectId(projectId)
            .category(dto.category())
            .contextKey(dto.contextKey())
            .content(dto.content())
            .contentHash(com.qaassist.util.HashUtils.sha256(dto.content()))
            .version(dto.version())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build())
        .toList();

    repository.saveAll(entities);
    log.info("Synced {} context entries for project {}", entities.size(), projectId);
  }

  /**
   * Основной метод получения контекста для агентов.
   * Работает с кэшем, фильтрует и обрезает под лимиты LLM.
   */
  @Cacheable(value = "global-context", key = "#query.projectId() + ':' + #query.categories()")
  public ContextSlice getContextSlice(ContextQuery query) {
    log.debug("Fetching context slice for project: {}, categories: {}",
        query.projectId(), query.categories());

    List<ContextEntryEntity> entities;
    if (query.categories().isEmpty()) {
      entities = repository.findByProjectIdAndCategoryIn(query.projectId(),
          Arrays.asList("architecture", "endpoints", "glossary", "test_data"));
    } else {
      entities = repository.findByProjectIdAndCategoryIn(query.projectId(), query.categories());
    }

    // Фильтрация по ключевым словам
    if (query.keywords().isPresent()) {
      var keywords = query.keywords().get();
      entities = entities.stream()
          .filter(e -> matchesAny(e.getContent(), keywords))
          .toList();
    }

    // Сортировка по дате обновления
    if (query.prioritizeRecent()) {
      entities = entities.stream()
          .sorted(Comparator.comparing(ContextEntryEntity::getUpdatedAt).reversed())
          .toList();
    }

    var dtos = entities.stream()
        .map(ContextEntryDto::fromEntity)
        .toList();

    return ContextSlice.of(query.projectId(), dtos, query.maxChars().orElse(8000));
  }

  private boolean matchesAny(String content, List<String> keywords) {
    if (keywords.isEmpty()) return true;
    String lower = content.toLowerCase();
    return keywords.stream().anyMatch(k -> lower.contains(k.toLowerCase()));
  }

  /**
   * Утилита для хеширования (вынесена отдельно для переиспользования)
   */
  private static class HashUtils {
    static String sha256(String input) {
      try {
        var md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes());
        return java.util.Base64.getUrlEncoder().encodeToString(digest);
      } catch (Exception e) {
        return UUID.randomUUID().toString();
      }
    }
  }
}