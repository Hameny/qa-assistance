package com.qaassist.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.Set;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.*;

class PromptEngineTest {

  private PromptRenderer renderer;
  private PromptValidator validator;
  private PromptRegistry registry;

  @BeforeEach
  void setUp() {
    renderer = new PromptRenderer();
    validator = new PromptValidator();

    // Имитация загрузки шаблона вручную для теста
    registry = new PromptRegistry() {
      {
        templates.put("decomp:v1.0", new PromptTemplate(
            "decomp", "v1.0",
            "You are a QA expert.",
            "Project: {{PROJECT}}\nRequirements: {{REQUIREMENTS}}",
            Set.of("PROJECT", "REQUIREMENTS"),
            4000,
            "Test template"
        ));
      }
    };
  }

  @org.testng.annotations.Test
  @DisplayName("Рендеринг с подстановкой переменных и defaults")
  void rendersWithVariablesAndDefaults() {
    String template = "Hello {{NAME}}, project is {{PROJECT:Unknown}}";
    String result = renderer.render(template, Map.of("NAME", "Alice"));

    assertThat(result).isEqualTo("Hello Alice, project is Unknown");
  }

  @ParameterizedTest
  @CsvSource({
      "```json\n{'a':1}\n```",  // Sanitizes code blocks
      "{{nested}}",             // Escapes nested braces
      "  trim me  "             // Trims whitespace
  })
  @DisplayName("Санитизация входных данных")
  void sanitizesInput(String input) {
    String template = "Data: {{INPUT}}";
    String result = renderer.render(template, Map.of("INPUT", input));
    assertThat(result).doesNotContain("```");
    assertThat(result).doesNotContain("{{nested}}");
  }

  @Test
  @DisplayName("Валидация: отсутствие обязательной переменной")
  void validationFailsOnMissingRequiredVar() {
    var template = registry.getTemplate("decomp");
    assertThatThrownBy(() -> validator.validate(template, Map.of("PROJECT", "QA"),
        renderer.render(template.userTemplate(), Map.of("PROJECT", "QA"))))
        .isInstanceOf(PromptValidationException.class)
        .hasMessageContaining("Missing required variable: REQUIREMENTS");
  }

  @Test
  @DisplayName("Валидация: превышение лимита токенов")
  void validationFailsOnTokenLimit() {
    var template = new PromptTemplate("small", "v1", "sys", "usr: {{DATA}}", Set.of(), 10, "tiny");
    String longData = "x".repeat(1000);
    String rendered = renderer.render(template.userTemplate(), Map.of("DATA", longData));

    assertThatThrownBy(() -> validator.validate(template, Map.of("DATA", longData), rendered))
        .hasMessageContaining("exceed limit");
  }
}