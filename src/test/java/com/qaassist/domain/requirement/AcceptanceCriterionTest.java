// src/test/java/com/qaassist/domain/requirement/AcceptanceCriterionTest.java
package com.qaassist.domain.requirement;

import com.qaassist.domain.common.Priority;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AcceptanceCriterionTest {

  @Test
  void toGherkinConversion() {
    var ac = new AcceptanceCriterion(
        UUID.randomUUID(),
        "Successful login",
        "User logs in with valid credentials",
        Priority.HIGH,
        "jira:PROJ-100",
        "User is on login page",
        "User enters valid credentials and clicks login",
        "User is redirected to dashboard",
        true
    );

    var gherkin = ac.toGherkin();
    assertThat(gherkin).contains("Scenario: Successful login");
    assertThat(gherkin).contains("Given User is on login page");
    assertThat(gherkin).contains("When User enters valid credentials and clicks login");
    assertThat(gherkin).contains("Then User is redirected to dashboard");
  }
}