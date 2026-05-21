package com.qaassist.data;

import com.qaassist.domain.artifact.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestDataEnricher {

  private final TestDataResolver resolver;

  /**
   * Обогащает весь TestSuite тестовыми данными.
   */
  public TestSuite enrich(TestSuite suite, String projectId) {
    log.info("💉 Enriching test suite '{}' with data for project {}", suite.suiteName(), projectId);

    List<TestCase> enrichedCases = suite.testCases().stream()
        .map(tc -> enrichTestCase(tc, projectId))
        .toList();

    return new TestSuite(
        suite.id(), suite.suiteName(), suite.sourceStoryId(),
        enrichedCases, suite.generatedAt(), suite.metadata()
    );
  }

  private TestCase enrichTestCase(TestCase tc, String projectId) {
    List<TestStep> enrichedSteps = tc.steps().stream()
        .map(step -> {
          String action = resolver.resolve(step.action(), projectId);
          String expected = resolver.resolve(step.expected(), projectId);

          // Обогащаем вложенные структуры
          TestStep.ApiCall enrichedApi = step.apiCall().map(api ->
              new TestStep.ApiCall(
                  api.method(),
                  resolver.resolve(api.endpoint(), projectId),
                  api.requestBody().map(body -> resolver.resolve(body, projectId)),
                  api.headers().map(headers -> headers.stream()
                      .map(h -> new TestStep.Header(h.name(), resolver.resolve(h.value(), projectId)))
                      .toList())
              )
          );

          TestStep.UiLocator enrichedUi = step.uiLocator().map(ui ->
              new TestStep.UiLocator(
                  resolver.resolve(ui.selector(), projectId),
                  ui.type(),
                  ui.frame().map(f -> resolver.resolve(f, projectId)),
                  ui.description()
              )
          );

          List<Assertion> enrichedAsserts = step.assertions().stream()
              .map(a -> new Assertion(
                  a.id(),
                  resolver.resolve(a.description(), projectId),
                  a.type(),
                  resolver.resolve(a.expectedValue(), projectId),
                  resolver.resolve(a.actualExpression(), projectId),
                  a.critical()
              ))
              .toList();

          return new TestStep(
              step.id(), action, expected, step.estimatedDurationSeconds(),
              enrichedAsserts, step.testDataRef(), enrichedUi, enrichedApi
          );
        })
        .toList();

    return new TestCase(
        tc.id(), resolver.resolve(tc.name(), projectId), resolver.resolve(tc.description(), projectId),
        tc.priority(), tc.type(), enrichedSteps, tc.traceability(),
        tc.createdAt(), tc.updatedAt(), tc.linkedRequirements()
    );
  }
}