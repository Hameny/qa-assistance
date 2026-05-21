package com.qaassist.data;

import com.qaassist.domain.artifact.TestCase;
import com.qaassist.domain.artifact.TestSuite;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class DataIntegrityValidator {

  private static final Pattern UNRESOLVED_REF = Pattern.compile("\\{\\{REF:(\\w+)\\}\\}");

  /**
   * Проверяет, не остались ли неразрешенные ссылки {{REF:...}} после enrichment.
   */
  public ValidationResult validate(TestSuite suite, TestDataResolver resolver) {
    Set<String> unresolved = new HashSet<>();
    Set<String> allKeys = resolver.getGeneratedValues().keySet();

    for (TestCase tc : suite.testCases()) {
      for (var step : tc.steps()) {
        checkField(step.action(), unresolved);
        checkField(step.expected(), unresolved);
        step.apiCall().ifPresent(api -> {
          api.requestBody().ifPresent(b -> checkField(b, unresolved));
          api.headers().ifPresent(headers -> headers.forEach(h -> checkField(h.value(), unresolved)));
        });
      }
    }

    boolean valid = unresolved.isEmpty();
    if (!valid) {
      log.warn("⚠️ Unresolved references found: {}", unresolved);
    }
    return new ValidationResult(valid, unresolved);
  }

  private void checkField(String value, Set<String> unresolved) {
    if (value == null) return;
    Matcher m = UNRESOLVED_REF.matcher(value);
    while (m.find()) {
      unresolved.add(m.group(1));
    }
  }

  public record ValidationResult(boolean valid, Set<String> unresolvedRefs) {}
}