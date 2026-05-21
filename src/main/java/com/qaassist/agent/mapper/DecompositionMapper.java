package com.qaassist.agent.mapper;

import com.qaassist.agent.dto.LlmDecompositionResponse;
import com.qaassist.domain.common.Priority;
import com.qaassist.domain.requirement.AcceptanceCriterion;
import com.qaassist.domain.requirement.UserStory;
import com.qaassist.domain.requirement.Requirement;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class DecompositionMapper {

  public UserStory toDomain(LlmDecompositionResponse llmResponse, String sourceJiraKey) {
    Priority priority = safeMapPriority(llmResponse.priority());

    List<AcceptanceCriterion> criteria = llmResponse.acceptanceCriteria().stream()
        .map(acText -> parseAcceptanceCriterion(acText, sourceJiraKey))
        .toList();

    return new UserStory(
        UUID.randomUUID(),
        llmResponse.title(),
        llmResponse.description(),
        priority,
        sourceJiraKey,
        criteria,
        llmResponse.businessRules() != null ? llmResponse.businessRules() : List.of(),
        Instant.now(),
        Instant.now()
    );
  }

  private Priority safeMapPriority(String raw) {
    return switch (raw.toUpperCase()) {
      case "CRITICAL" -> Priority.CRITICAL;
      case "HIGH" -> Priority.HIGH;
      case "LOW" -> Priority.LOW;
      default -> Priority.MEDIUM;
    };
  }

  private AcceptanceCriterion parseAcceptanceCriterion(String text, String sourceKey) {
    // Простой парсинг Gherkin-подобного текста: Given... When... Then...
    String given = extractSection(text, "Given", "When");
    String when = extractSection(text, "When", "Then");
    String then = extractSection(text, "Then", null);

    return new AcceptanceCriterion(
        UUID.randomUUID(),
        "AC: " + text.substring(0, Math.min(50, text.length())),
        text,
        Priority.MEDIUM,
        sourceKey,
        given != null ? given : text,
        when != null ? when : "",
        then != null ? then : "",
        true
    );
  }

  private String extractSection(String text, String startMarker, String endMarker) {
    int start = text.indexOf(startMarker);
    if (start == -1) return null;
    start += startMarker.length();
    int end = endMarker != null ? text.indexOf(endMarker, start) : text.length();
    return text.substring(start, end).trim();
  }
}