package com.qaassist.validation.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaassist.llm.LlmClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LlmJsonParserServiceTest {

  private LlmJsonParserService parser;
  private LlmClientService mockLlmClient;
  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    SchemaRegistry registry = new SchemaRegistry();
    ReflectionTestUtils.invokeMethod(registry, "loadSchemas"); // Загрузка схем для теста

    JsonSchemaValidator validator = new JsonSchemaValidator(registry);
    mapper = new ObjectMapper();
    parser = new LlmJsonParserService(validator, mapper);

    mockLlmClient = mock(LlmClientService.class);
    LlmJsonParserService.LlmClientServiceHolder.setClient(mockLlmClient);
  }

  @Test
  @DisplayName("Успешный парсинг валидного JSON")
  void parsesValidJson() {
    String validJson = """
            {
              "title": "User Login",
              "description": "As a user I want to login",
              "priority": "HIGH",
              "acceptanceCriteria": ["Valid credentials", "Invalid shows error"]
            }
            """;

    var result = parser.parseWithRecovery(validJson, "user-story", UserStoryDto.class, "", "");
    assertThat(result.title()).isEqualTo("User Login");
    assertThat(result.priority()).isEqualTo("HIGH");
  }

  @Test
  @DisplayName("Recovery при битом JSON")
  void recoveryOnInvalidJson() {
    String invalidJson = """
            {
              "title": "Broken Story",
              "priority": "INVALID_PRIO"
            }
            """;

    when(mockLlmClient.chat(any(), any(), any())).thenReturn("""
            {
              "title": "Fixed Story",
              "description": "Recovery worked",
              "priority": "MEDIUM",
              "acceptanceCriteria": ["Test 1"]
            }
            """);

    var result = parser.parseWithRecovery(invalidJson, "user-story", UserStoryDto.class, "sys", "usr");
    assertThat(result.title()).isEqualTo("Fixed Story");
    verify(mockLlmClient, times(1)).chat(any(), contains("VALIDATION ERROR"), any());
  }

  @Test
  @DisplayName("Выброс исключения при повторной ошибке recovery")
  void throwsOnDoubleFailure() {
    String badJson = "{\"title\": \"A\"}";
    when(mockLlmClient.chat(any(), any(), any())).thenReturn("{\"title\": \"B\"}"); // тоже invalid

    assertThatThrownBy(() -> parser.parseWithRecovery(badJson, "user-story", UserStoryDto.class, "s", "u"))
        .isInstanceOf(LlmParseException.class)
        .hasMessageContaining("Failed to generate valid JSON after recovery");
  }

  // Вспомогательный DTO для теста (в реальном проекте используйте UserStory)
  record UserStoryDto(String title, String description, String priority, String[] acceptanceCriteria) {}
}