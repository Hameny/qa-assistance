package com.qaassist.agent;

import com.qaassist.agent.dto.LlmDecompositionResponse;
import com.qaassist.llm.LlmClientService;
import com.qaassist.prompt.PromptService;
import com.qaassist.prompt.model.RenderedPrompt;
import com.qaassist.validation.schema.LlmJsonParserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequirementsDecomposerAgentTest {

  @Mock private PromptService promptService;
  @Mock private LlmClientService llmClient;
  @Mock private LlmJsonParserService jsonParser;
  @InjectMocks private RequirementsDecomposerAgent agent;

  @Test
  @DisplayName("Успешная декомпозиция: полный цикл")
  void successfulDecompositionFlow() {
    // Arrange
    when(promptService.preparePrompt(anyString(), anyMap()))
        .thenReturn(new RenderedPrompt("decomp", "v1", "sys", "usr", 4000));
    when(llmClient.chat(anyString(), anyString(), anyMap()))
        .thenReturn("{\"title\":\"Login\",\"description\":\"User login\",\"priority\":\"HIGH\",\"acceptanceCriteria\":[\"Given user is on login page When enters valid creds Then redirected to dashboard\"],\"discrepancies\":[],\"businessRules\":[\"Password min 8 chars\"]}");
    when(jsonParser.parseWithRecovery(anyString(), anyString(), any(), anyString(), anyString()))
        .thenReturn(new LlmDecompositionResponse(
            "Login", "User login", "HIGH",
            List.of("Given... When... Then..."),
            List.of("Password min 8 chars"),
            List.of(),
            List.of()
        ));

    // Act
    var result = agent.decompose("PROJ-1", "Raw reqs...", "Context...");

    // Assert
    assertThat(result.userStory().title()).isEqualTo("Login");
    assertThat(result.userStory().priority().weight()).isEqualTo(3); // HIGH
    assertThat(result.userStory().acceptanceCriteria()).hasSize(1);
    assertThat(result.validationWarnings()).isEmpty();
    assertThat(result.discrepancies()).isEmpty();

    verify(promptService).preparePrompt(eq("requirements_decomposition"), anyMap());
    verify(llmClient).chat(anyString(), anyString(), eq(Map.of()));
    verify(jsonParser).parseWithRecovery(anyString(), eq("user-story"), eq(LlmDecompositionResponse.class), anyString(), anyString());
  }

  @Test
  @DisplayName("Обнаружение критических расхождений")
  void detectsCriticalDiscrepancies() {
    when(promptService.preparePrompt(any(), any())).thenReturn(mock(RenderedPrompt.class));
    when(llmClient.chat(any(), any(), any())).thenReturn("response");
    when(jsonParser.parseWithRecovery(any(), any(), any(), any(), any()))
        .thenReturn(new LlmDecompositionResponse(
            "Broken Req", "Missing flow", "MEDIUM",
            List.of("AC1"),
            List.of(),
            List.of(new LlmDecompositionResponse.LlmDiscrepancy("CONTRADICTION", "States A and B simultaneously", "HIGH")),
            List.of()
        ));

    var result = agent.decompose("PROJ-2", "req", "ctx");

    assertThat(result.hasCriticalDiscrepancies()).isTrue();
    assertThat(result.discrepancies()).hasSize(1);
  }

  @Test
  @DisplayName("Бизнес-валидация: предупреждения при малом количестве AC")
  void validationWarningsForMissingAC() {
    when(promptService.preparePrompt(any(), any())).thenReturn(mock(RenderedPrompt.class));
    when(llmClient.chat(any(), any(), any())).thenReturn("response");
    when(jsonParser.parseWithRecovery(any(), any(), any(), any(), any()))
        .thenReturn(new LlmDecompositionResponse(
            "Short Req", "No details", "CRITICAL",
            List.of("Only one AC"),
            List.of(),
            List.of(),
            List.of()
        ));

    var result = agent.decompose("PROJ-3", "req", "ctx");
    assertThat(result.validationWarnings()).hasSize(1);
    assertThat(result.validationWarnings().get(0)).contains("CRITICAL story should have at least 2");
  }
}