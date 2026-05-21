// src/test/java/com/qaassist/agent/comparator/AutomationComparatorTest.java
package com.qaassist.agent.comparator;

import com.qaassist.agent.comparator.model.ComparisonReport;
import com.qaassist.domain.artifact.*;
import com.qaassist.domain.common.Priority;
import com.qaassist.domain.common.TestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AutomationComparatorTest {

  @Autowired private AutomationComparator comparator;

  @Test
  @DisplayName("Успешное сравнение: 100% покрытие")
  void fullMatchCoverage() {
    TestSuite suite = createTestSuite("Login", List.of(
        createStep("Enter user", "POST", "/login", "{\"user\":\"admin\"}"),
        createStep("Check response", "GET", "/status", null)
    ));

    String code = """
            import org.junit.jupiter.api.Test;
            @Test
            @DisplayName("Login")
            void loginTest() {
                given().post("/login");
                then().statusCode(200);
            }
            """;

    ComparisonReport report = comparator.compare(suite, code);
    assertThat(report.coveragePercent()).isEqualTo(100.0);
    assertThat(report.overallStatus()).isEqualTo(ComparisonReport.ComparisonStatus.FULL_MATCH);
    assertThat(report.discrepancies()).isEmpty();
  }

  @Test
  @DisplayName("Частичное покрытие: пропущен шаг")
  void partialCoverage() {
    TestSuite suite = createTestSuite("Get Users", List.of(
        createStep("Fetch list", "GET", "/users", null),
        createStep("Validate count", "GET", "/users/count", null)
    ));

    String code = """
            @Test
            @DisplayName("Get Users")
            void getUsers() {
                get("/users");
            }
            """;

    ComparisonReport report = comparator.compare(suite, code);
    assertThat(report.coveragePercent()).isLessThan(100.0);
    assertThat(report.discrepancies()).hasSize(1);
    assertThat(report.discrepancies().get(0).description()).contains("not implemented");
  }

  private TestSuite createTestSuite(String name, List<TestStep> steps) {
    var tc = new TestCase(UUID.randomUUID(), name, name + " flow", Priority.HIGH, TestType.FUNCTIONAL,
        steps, null, Instant.now(), Instant.now(), List.of());
    return new TestSuite(UUID.randomUUID(), name, "s-1", List.of(tc), Instant.now(), new TestSuite.GenerationMetadata(List.of(), List.of()));
  }

  private TestStep createStep(String action, String method, String endpoint, String body) {
    var api = new TestStep.ApiCall(method, endpoint,
        java.util.Optional.ofNullable(body), java.util.Optional.empty());
    return new TestStep(UUID.randomUUID(), action, "OK", 5,
        List.of(), java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.of(api));
  }
}