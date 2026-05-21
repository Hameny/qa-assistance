package com.qaassist.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PromptRegistry {

  private final Map<String, PromptTemplate> templates = new ConcurrentHashMap<>();
  private final ObjectMapper yamlMapper;

  public PromptRegistry() {
    this.yamlMapper = new ObjectMapper(new YAMLFactory());
  }

  @PostConstruct
  public void loadPrompts() throws IOException {
    var resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resolver.getResources("classpath:prompts/*.yaml");

    for (Resource res : resources) {
      try {
        PromptTemplate template = yamlMapper.readValue(res.getInputStream(), PromptTemplate.class);
        String key = template.id() + ":" + template.version();
        templates.put(key, template);
        log.info("Loaded prompt template: {} (version: {})", template.id(), template.version());
      } catch (Exception e) {
        log.error("Failed to load prompt from {}: {}", res.getFilename(), e.getMessage());
      }
    }
  }

  /** Получает шаблон по ID. Если версий несколько — возвращает последнюю стабильную. */
  public PromptTemplate getTemplate(String templateId) {
    // Простая логика: ищем все версии, сортируем, берём последнюю не-experimental
    return templates.values().stream()
        .filter(t -> t.id().equals(templateId))
        .filter(t -> !t.isExperimental())
        .max((a, b) -> a.version().compareTo(b.version()))
        .orElseThrow(() -> new IllegalArgumentException(
            "No stable prompt template found for ID: " + templateId
        ));
  }

  /** Получает конкретную версию (для A/B тестов или отката) */
  public PromptTemplate getTemplateVersion(String templateId, String version) {
    String key = templateId + ":" + version;
    return templates.get(key);
  }

  /** Возвращает все доступные ID шаблонов */
  public Set<String> getAvailableTemplateIds() {
    return templates.values().stream()
        .map(PromptTemplate::id)
        .collect(java.util.stream.Collectors.toSet());
  }
}