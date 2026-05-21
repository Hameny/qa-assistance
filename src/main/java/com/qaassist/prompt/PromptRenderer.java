package com.qaassist.prompt;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PromptRenderer {

  // Поддержка синтаксиса {{VAR}} и {{VAR:default_value}}
  private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(\\w+)(?::([^}]+))?\\}\\}");

  /**
   * Рендерит шаблон, подставляя переменные.
   * Поддерживает значения по умолчанию: {{PROJECT:MyApp}}
   */
  public String render(String template, Map<String, Object> variables) {
    if (variables == null || variables.isEmpty()) return template;

    Matcher matcher = VAR_PATTERN.matcher(template);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
      String varName = matcher.group(1);
      String defaultValue = matcher.group(2);

      Object value = variables.get(varName);
      String replacement = (value != null)
          ? sanitize(value.toString())
          : (defaultValue != null ? defaultValue : "");

      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  /** Базовая санитизация: обрезка пробелов, экранирование Markdown-инъекций */
  private String sanitize(String input) {
    return input.trim()
        .replace("```", "\\`\\`\\`")  // предотвращаем разрыв JSON-блоков
        .replace("{{", "\\{\\{");     // защита от вложенных шаблонов
  }
}