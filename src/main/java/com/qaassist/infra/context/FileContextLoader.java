package com.qaassist.infra.context;

import com.qaassist.domain.context.ContextEntryDto;
import com.qaassist.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileContextLoader {

  private final AppProperties properties;
  private final Yaml yamlParser = new Yaml();

  public List<ContextEntryDto> loadFromFiles(String projectId) {
    Path baseDir = Paths.get(properties.getGlobalContext().getBasePath(), projectId);
    if (!Files.exists(baseDir)) {
      log.warn("Global context directory not found for project: {}", projectId);
      return List.of();
    }

    try (Stream<Path> paths = Files.walk(baseDir)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(p -> isSupportedFile(p))
          .map(this::parseFile)
          .filter(Objects::nonNull)
          .toList();
    } catch (IOException e) {
      log.error("Failed to load context files for project {}", projectId, e);
      return List.of();
    }
  }

  private boolean isSupportedFile(Path path) {
    String name = path.getFileName().toString().toLowerCase();
    return name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".md") || name.endsWith(".txt");
  }

  private ContextEntryDto parseFile(Path path) {
    try {
      String content = Files.readString(path);
      String hash = computeHash(content);
      String relativePath = Paths.get(properties.getGlobalContext().getBasePath())
          .relativize(path).toString().replace("\\", "/");

      // Определяем категорию из имени файла или директории
      String category = extractCategory(path);
      String key = path.getFileName().toString().replaceAll("\\.(yml|yaml|md|txt)$", "");

      return new ContextEntryDto(
          null, // id генерируется при сохранении
          relativePath.split("/")[0], // projectId берём из первой папки
          category,
          key,
          content,
          1,
          null
      );
    } catch (IOException e) {
      log.error("Error reading file {}", path, e);
      return null;
    }
  }

  private String extractCategory(Path path) {
    String parent = path.getParent().getFileName().toString();
    return switch (parent.toLowerCase()) {
      case "endpoints", "api" -> "endpoints";
      case "data", "fixtures" -> "test_data";
      case "docs", "architecture" -> "architecture";
      case "glossary", "terms" -> "glossary";
      case "specs", "openapi" -> "openapi";
      default -> "misc";
    };
  }

  private String computeHash(String content) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(content.getBytes());
      return Base64.getUrlEncoder().encodeToString(digest);
    } catch (Exception e) {
      return UUID.randomUUID().toString();
    }
  }
}