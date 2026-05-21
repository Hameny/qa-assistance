// src/test/java/com/qaassist/agent/ui/UiAutomationSkillTest.java
package com.qaassist.agent.ui;

import com.qaassist.agent.ui.engine.PlaywrightTemplateEngine;
import com.qaassist.data.TestDataResolver;
import com.qaassist.domain.artifact.*;
import com.qaassist.domain.common.Priority;
import com.qaassist.domain.common.TestType;
import com.qaassist.domain.selector.SelectorCatalog;
import com.qaassist.domain.selector.UiLocator;
import com.qaassist.service.SelectorCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UiAutomationSkillTest {

  @Mock private TestDataResolver dataResolver;
  @Mock private SelectorCacheService selectorCache;
  @InjectMocks private UiAutomationSkill skill;

  @Test
  @DisplayName("Генерация Page Object + Test класса")
  void generatesPageObjectAndTestClass() {
    // Arrange
    when(dataResolver.resolve(anyString(), anyString())).thenAnswer(i -> i.getArgument(0));
    when(selectorCache.loadCatalog(anyString())).thenReturn(createMockCatalog());

    TestSuite suite = createMockUiSuite();

    // Act
    var result = skill.generate(suite, "PROJ-UI", "https://app.test.com");

    // Assert
    assertThat(result.pageClassName()).endsWith("Page");
    assertThat(result.testClassName()).endsWith("Test");

    assertThat(result.pageSource()).contains("private final Page page;");
    assertThat(result.pageSource()).contains("public void enterUsername(String value)");
    assertThat(result.pageSource()).contains("page.getByTestId(\"username-input\")");

    assertThat(result.testSource()).contains("@UsePlaywright");
    assertThat(result.testSource()).contains("LoginPage loginPage = new LoginPage(page);");
    assertThat(result.testSource()).contains("loginPage.enterUsername(\"{{STATIC:users.admin.login}}\")");
    assertThat(result.testSource()).contains("assertThat(loginPage.getSubmitButton()).isVisible()");
  }

  private SelectorCatalog createMockCatalog() {
    UiLocator username = new UiLocator(UUID.randomUUID(), "username-input",
        UiLocator.LocatorType.TEST_ID, "Locator for: username", List.of(),
        new UiLocator.StabilityScore(95, List.of("Has data-testid")), null, true, null);
    UiLocator submit = new UiLocator(UUID.randomUUID(), "submit-btn",
        UiLocator.LocatorType.TEST_ID, "Locator for: submitButton", List.of(),
        new UiLocator.StabilityScore(95, List.of()), null, true, null);

    return new SelectorCatalog(UUID.randomUUID(), "PROJ-UI", Map.of(),
        Map.of("username", List.of(username), "submitButton", List.of(submit)),
        Instant.now(), new SelectorCatalog.CatalogMetadata(2, 2, 0, "1.44.0", List.of()));
  }

  private TestSuite createMockUiSuite() {
    var uiLoc = new TestStep.UiLocator("#username", UiLocator.LocatorType.CSS_CLASS, java.util.Optional.empty(), "Locator for: username");
    var step1 = new TestStep(UUID.randomUUID(), "Enter username", "Field filled", 3,
        List.of(), java.util.Optional.of("{{STATIC:users.admin.login}}"), java.util.Optional.of(uiLoc), java.util.Optional.empty());

    var uiBtn = new TestStep.UiLocator("#submit", UiLocator.LocatorType.CSS_CLASS, java.util.Optional.empty(), "Locator for: submitButton");
    var step2 = new TestStep(UUID.randomUUID(), "Click submit", "Navigated to dashboard", 2,
        List.of(new Assertion(UUID.randomUUID(), "Button visible", Assertion.AssertionType.IS_PRESENT, "true", "$.visible", true)),
        java.util.Optional.empty(), java.util.Optional.of(uiBtn), java.util.Optional.empty());

    var tc = new TestCase(UUID.randomUUID(), "Login UI", "User logs in via UI", Priority.HIGH, TestType.FUNCTIONAL,
        List.of(step1, step2), null, Instant.now(), Instant.now(), List.of());

    return new TestSuite(UUID.randomUUID(), "Login", "story-1", List.of(tc), Instant.now(), new TestSuite.GenerationMetadata(List.of(), List.of()));
  }
}