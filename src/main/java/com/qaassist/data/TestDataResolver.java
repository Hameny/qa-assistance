package com.qaassist.data;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TestDataResolver {

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
      "\\{\\{(\\w+)(?::([^}]*))?\\}\\}", Pattern.CASE_INSENSITIVE
  );

  private final FixtureLoader fixtureLoader;
  private final ThreadLocal<Map<String, String>> runContext = ThreadLocal.withInitial(ConcurrentHashMap::new);

  public TestDataResolver(FixtureLoader fixtureLoader) {
    this.fixtureLoader = fixtureLoader;
  }

  /**
   * Резолвит все плейсхолдеры в строке.
   * Поддерживает: {{UUID}}, {{TIMESTAMP}}, {{RANDOM_EMAIL}}, {{STATIC:users.admin}}, {{REF:generated_key}}
   */
  public String resolve(String input, String projectId) {
    if (input == null || !input.contains("{{")) return input;

    Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
      String type = matcher.group(1).toUpperCase();
      String param = matcher.group(2);
      String replacement = resolveToken(type, param, projectId);
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  /** Очистка контекста выполнения (вызывать после завершения тест-рана) */
  public void clearRunContext() { runContext.remove(); }

  /** Получить сгенерированные за ран значения (для отладки/логирования) */
  public Map<String, String> getGeneratedValues() { return Map.copyOf(runContext.get()); }

  private String resolveToken(String type, String param, String projectId) {
    return switch (type) {
      case "UUID" -> generateAndCache("UUID", UUID.randomUUID().toString());
      case "TIMESTAMP" -> generateAndCache("TIMESTAMP", Instant.now().toString());
      case "RANDOM_EMAIL" -> generateAndCache("EMAIL",
          "test_" + UUID.randomUUID().toString().substring(0, 8) + "@qa-assist.local");
      case "RANDOM_PHONE" -> generateAndCache("PHONE", "+7900" + (1000000 + new Random().nextInt(9000000)));
      case "STATIC" -> resolveStatic(param, projectId);
      case "REF" -> resolveReference(param);
      default -> {
        log.warn("⚠️ Unknown placeholder type: {}", type);
        yield "{{" + type + (param != null ? ":" + param : "") + "}}";
      }
    };
  }

  private String resolveStatic(String path, String projectId) {
    if (path == null || path.isBlank()) return "";
    Map<String, Object> fixtures = fixtureLoader.loadStaticFixtures(projectId);

    String[] keys = path.split("\\.");
    Object current = fixtures;
    for (String key : keys) {
      if (current instanceof Map<?, ?> map) {
        current = map.get(key);
      } else {
        return "{{STATIC:" + path + "}}"; // Не найдено
      }
    }
    return current != null ? current.toString() : "";
  }

  private String resolveReference(String key) {
    return runContext.get().getOrDefault(key, "{{REF:" + key + "}}");
  }

  private String generateAndCache(String key, String value) {
    runContext.get().put(key, value);
    return value;
  }
}