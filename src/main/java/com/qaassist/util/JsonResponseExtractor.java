package com.qaassist.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonResponseExtractor {

  private JsonResponseExtractor() {}

  private static final Pattern JSON_CODE_BLOCK = Pattern.compile(
      "```(?:json)?\\s*([\s\S]*?)\\s*```",
      Pattern.DOTALL | Pattern.CASE_INSENSITIVE
  );

  /**
   * Извлекает первый валидный JSON-объект или массив из ответа LLM.
   * Обрабатывает markdown-блоки, пробелы и мусор вокруг.
   */
  public static String extractJson(String rawResponse) {
    if (rawResponse == null || rawResponse.isBlank()) return null;

    // 1. Пробуем найти markdown-блок
    Matcher matcher = JSON_CODE_BLOCK.matcher(rawResponse);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }

    // 2. Если блока нет, ищем первую '{' или '[' и последнюю '}' или ']'
    int start = Math.max(rawResponse.indexOf('{'), rawResponse.indexOf('['));
    int end = Math.max(rawResponse.lastIndexOf('}'), rawResponse.lastIndexOf(']'));

    if (start != -1 && end > start) {
      return rawResponse.substring(start, end + 1).trim();
    }

    return null;
  }

  /**
   * Проверяет, является ли строка валидным JSON (объект или массив).
   */
  public static boolean isValidJson(String json) {
    if (json == null || json.isBlank()) return false;
    String trimmed = json.trim();
    return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
        (trimmed.startsWith("[") && trimmed.endsWith("]"));
  }
}