// src/test/java/com/qaassist/service/GlobalContextServiceIntegrationTest.java
package com.qaassist.service;

import com.qaassist.domain.context.ProjectContextEntity;
import com.qaassist.domain.context.ProjectContextRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class GlobalContextServiceIntegrationTest {

  @Autowired
  private GlobalContextService contextService;

  @Autowired
  private ProjectContextRepository repository;

  @Autowired
  private CacheManager cacheManager;

  private static final String PROJECT_ID = "PROJ-AUTH-2024";

  @BeforeEach
  void setUp() {
    // Очистка кэша и БД для изоляции тестов
    cacheManager.getCache("global-context").clear();
    repository.deleteAll();

    // Seed данных
    repository.saveAll(List.of(
        ProjectContextEntity.builder()
            .projectId(PROJECT_ID)
            .type(ProjectContextEntity.ContextType.ENDPOINTS)
            .content("auth_url=https://auth.dev/api/v2\nuser_service=https://users.dev/api/v1")
            .sourceRef("confluence:ARCH-101")
            .isActive(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build(),
        ProjectContextEntity.builder()
            .projectId(PROJECT_ID)
            .type(ProjectContextEntity.ContextType.GLOSSARY)
            .content("Tenant = Организация\nSuperAdmin = Глобальный админ")
            .sourceRef("jira:PROJ-456")
            .isActive(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build()
    ));
  }

  @Test
  @DisplayName("Загрузка и кэширование полного контекста")
  void loadsAndCachesFullContext() {
    Map<String, String> ctx1 = contextService.loadFullContext(PROJECT_ID);
    assertThat(ctx1).containsKeys("endpoints", "glossary");
    assertThat(ctx1.get("endpoints")).contains("auth_url");

    // Второй вызов должен вернуть тот же объект из кэша
    Map<String, String> ctx2 = contextService.loadFullContext(PROJECT_ID);
    assertThat(ctx2).isSameAs(ctx1);
  }

  @Test
  @DisplayName("Генерация оптимизированного среза")
  void generatesOptimizedContextSlice() {
    String slice = contextService.getContextSlice(
        PROJECT_ID,
        List.of(ProjectContextEntity.ContextType.ENDPOINTS, ProjectContextEntity.ContextType.GLOSSARY)
    );

    assertThat(slice).contains("### ENDPOINTS");
    assertThat(slice).contains("### GLOSSARY");
    assertThat(slice.length()).isLessThanOrEqualTo(8000);
  }

  @Test
  @DisplayName("Сохранение обновляет кэш и создаёт новую версию")
  void saveContextInvalidatesCacheAndCreatesNewEntry() {
    // Сохраняем новые эндпоинты
    contextService.saveContext(PROJECT_ID, ProjectContextEntity.ContextType.ENDPOINTS,
        "new_endpoint=https://api.v3.com", "manual-update");

    // Проверяем инвалидацию кэша
    Map<String, String> newCtx = contextService.loadFullContext(PROJECT_ID);
    assertThat(newCtx.get("endpoints")).contains("api.v3.com");

    // Проверяем soft-delete старой записи
    long activeCount = repository.findByProjectIdAndIsActiveTrue(PROJECT_ID).stream()
        .filter(e -> e.getType() == ProjectContextEntity.ContextType.ENDPOINTS)
        .count();
    assertThat(activeCount).isEqualTo(1);
  }
}