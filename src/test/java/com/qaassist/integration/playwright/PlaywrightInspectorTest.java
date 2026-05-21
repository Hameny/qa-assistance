// src/test/java/com/qaassist/integration/playwright/PlaywrightInspectorTest.java
package com.qaassist.integration.playwright;

import com.microsoft.playwright.Playwright;
import com.qaassist.domain.selector.UiLocator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class PlaywrightInspectorTest {

  @Autowired
  private PlaywrightInspector inspector;

  @Autowired
  private Playwright playwright; // Инжектится через @Bean в тест-конфиге

  @BeforeEach
  void setUp() {
    // Убедиться, что браузер установлен для тестов
    // playwright.chromium().install()
  }

  @Test
  @DisplayName("Извлечение локаторов с тестовой страницы")
  void extractsLocatorsFromTestPage() {
    // Arrange: запустить локальный mock-сервер с тестовой страницей
    // (в реальном тесте использовать WireMock + static file serving)

    // Act
    List<UiLocator> locators = inspector.inspectPage(
        "http://localhost:8080/test-page",
        List.of("login-button", "username-input")
    );

    // Assert
    assertThat(locators).isNotEmpty();
    assertThat(locators.get(0).value()).matches("\\[data-testid=.*]|\\.btn-.*|//.*");
    assertThat(locators.get(0).stability().value()).isGreaterThan(40);
  }

  @Test
  @DisplayName("Приоритизация: data-testid > CSS > XPath")
  void prioritizesTestIdOverOtherSelectors() {
    List<UiLocator> candidates = List.of(
        new UiLocator(null, "//div[3]/button", UiLocator.LocatorType.XPATH, "desc",
            List.of(), new UiLocator.StabilityScore(40, List.of()), null, false, null),
        new UiLocator(null, ".btn-primary", UiLocator.LocatorType.CSS_CLASS, "desc",
            List.of(), new UiLocator.StabilityScore(70, List.of()), null, false, null),
        new UiLocator(null, "[data-testid='submit']", UiLocator.LocatorType.TEST_ID, "desc",
            List.of(), new UiLocator.StabilityScore(95, List.of()), null, true, null)
    );

    UiLocator best = candidates.stream()
        .max((a, b) -> Integer.compare(a.priorityScore(), b.priorityScore()))
        .orElseThrow();

    assertThat(best.type()).isEqualTo(UiLocator.LocatorType.TEST_ID);
    assertThat(best.isPreferred()).isTrue();
  }
}