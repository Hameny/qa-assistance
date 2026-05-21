package com.qaassist.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.qaassist.service.GlobalContextService;
import com.qaassist.domain.context.ProjectContextEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixtureLoader {

  private final GlobalContextService contextService;
  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  /**
   * Загружает и кэширует статические фикстуры проекта.
   * Ожидает YAML-структуру в разделе TEST_DATA контекста.
   */
  @Cacheable(value = "fixtures", key = "#projectId")
  public Map<String, Object> loadStaticFixtures(String projectId) {
    String rawData = contextService.getContextSlice(
        projectId,
        java.util.List.of(ProjectContextEntity.ContextType.TEST_DATA)
    );

    if (rawData.isBlank()) {
      log.debug("📭 No TEST_DATA context found for {}", projectId);
      return Collections.emptyMap();
    }

    try {
      // Убираем markdown-обёртку из GlobalContextService (### TEST_DATA\n...)
      String cleanYaml = rawData.replaceFirst("(?i)^.*?### TEST_DATA\\s*", "").trim();
      Map<String, Object> fixtures = yamlMapper.readValue(cleanYaml, new TypeReference<>() {});
      log.info("📦 Loaded {} fixture groups for {}", fixtures.size(), projectId);
      return fixtures;
    } catch (Exception e) {
      log.error("❌ Failed to parse TEST_DATA YAML: {}", e.getMessage());
      return Collections.emptyMap();
    }
  }
}