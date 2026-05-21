package com.qaassist.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Сырой ответ от LLM. Маппится на доменные объекты после валидации.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmDecompositionResponse(
    @NotBlank String title,
    @NotBlank String description,
    @NotBlank String priority,        // "CRITICAL", "HIGH", "MEDIUM", "LOW"
    @NotEmpty List<String> acceptanceCriteria,
    List<String> businessRules,
    List<LlmDiscrepancy> discrepancies,
    List<String> suggestedTestTypes   // e.g. ["FUNCTIONAL", "SECURITY"]
) {
  public record LlmDiscrepancy(
      @NotBlank String type,        // "AMBIGUITY", "CONTRADICTION", "MISSING_DETAIL"
      @NotBlank String description,
      @NotBlank String severity     // "HIGH", "MEDIUM", "LOW"
  ) {}
}