// src/main/java/com/qaassist/integration/playwright/PlaywrightInspector.java
package com.qaassist.integration.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.qaassist.domain.selector.UiLocator;
import com.qaassist.domain.selector.UiLocator.StabilityScore;
import com.qaassist.domain.selector.UiLocator.SelectorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaywrightInspector {

  private final Playwright playwright;
  private static final Pattern DYNAMIC_ID_PATTERN = Pattern.compile(".*-\\d{4,}.*|.*_[a-f0-9]{8}.*");

  /**
   * Инспектирует страницу и извлекает устойчивые селекторы для целевых элементов.
   */
  public List<UiLocator> inspectPage(String pageUrl, List<String> targetElements) {
    log.info("🔍 Inspecting page: {} for {} elements", pageUrl, targetElements.size());

    try (Browser browser = playwright.chromium().launch(
        new BrowserType.LaunchOptions().setHeadless(true).setTimeout(30000));
        BrowserContext context = browser.newContext();
        Page page = context.newPage()) {

      page.navigate(pageUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

      List<UiLocator> results = new ArrayList<>();
      for (String elementKey : targetElements) {
        List<UiLocator> locators = extractLocatorsForElement(page, elementKey);
        results.addAll(locators);
      }

      log.info("✅ Extracted {} locators from {}", results.size(), pageUrl);
      return results;

    } catch (Exception e) {
      log.error("❌ Failed to inspect page {}: {}", pageUrl, e.getMessage());
      return List.of();
    }
  }

  private List<UiLocator> extractLocatorsForElement(Page page, String elementKey) {
    List<UiLocator> candidates = new ArrayList<>();

    // 1. Пробуем найти по data-testid (наивысший приоритет)
    String testIdSelector = "[data-testid='%s']".formatted(elementKey);
    if (page.isVisible(testIdSelector)) {
      candidates.add(buildLocator(testIdSelector, UiLocator.LocatorType.TEST_ID, elementKey, 95, "Has data-testid"));
    }

    // 2. Пробуем ARIA role + name
    try {
      Locator roleLocator = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(elementKey));
      if (roleLocator.isVisible()) {
        candidates.add(buildLocator(roleLocator, UiLocator.LocatorType.ROLE, elementKey, 90, "ARIA role+name"));
      }
    } catch (Exception ignored) {}

    // 3. CSS по классам (исключаем динамические)
    String cssClass = findStableCssClass(page, elementKey);
    if (cssClass != null) {
      candidates.add(buildLocator(cssClass, UiLocator.LocatorType.CSS_CLASS, elementKey, 70, "Stable CSS class"));
    }

    // 4. XPath как fallback (наименьший приоритет)
    String xpath = generateRobustXPath(page, elementKey);
    if (xpath != null) {
      candidates.add(buildLocator(xpath, UiLocator.LocatorType.XPATH, elementKey, 40, "Generated XPath"));
    }

    // Сортируем по приоритету и возвращаем топ-3
    return candidates.stream()
        .sorted(Comparator.comparingInt(UiLocator::priorityScore).reversed())
        .limit(3)
        .toList();
  }

  private UiLocator buildLocator(String selector, UiLocator.LocatorType type,
      String elementKey, int stabilityValue, String factor) {
    return new UiLocator(
        UUID.randomUUID(),
        selector,
        type,
        "Locator for: " + elementKey,
        List.of(), // alternatives заполняются позже
        new StabilityScore(stabilityValue, List.of(factor)),
        new SelectorContext(null, null, List.of(), "1920x1080"),
        type == UiLocator.LocatorType.TEST_ID, // preferred только для test-id
        "auto-inspected"
    );
  }

  private UiLocator buildLocator(Locator locator, UiLocator.LocatorType type,
      String elementKey, int stabilityValue, String factor) {
    return new UiLocator(
        UUID.randomUUID(),
        locator.toString(),
        type,
        "Locator for: " + elementKey,
        List.of(),
        new StabilityScore(stabilityValue, List.of(factor)),
        new SelectorContext(null, null, List.of(), "1920x1080"),
        false,
        "auto-inspected"
    );
  }

  private String findStableCssClass(Page page, String elementKey) {
    // Ищем элементы, содержащие ключ в тексте/aria-label
    List<String> candidates = page.evaluate(
        """
        (key) => {
            const els = Array.from(document.querySelectorAll('*')).filter(el => 
                el.textContent?.includes(key) || 
                el.getAttribute('aria-label')?.includes(key) ||
                el.getAttribute('title')?.includes(key)
            );
            return els.map(el => {
                const classes = Array.from(el.classList).filter(c => 
                    !/^(css-[a-z0-9]{8}|_[a-z0-9]{6}|js-|is-)/.test(c)
                );
                return classes.length > 0 ? '.' + classes[0] : null;
            }).filter(Boolean);
        }
        """,
        elementKey
    );

    if (candidates instanceof List<?> list && !list.isEmpty()) {
      return (String) list.get(0);
    }
    return null;
  }

  private String generateRobustXPath(Page page, String elementKey) {
    // Генерация относительного XPath по тексту/атрибутам
    return page.evaluate(
        """
        (key) => {
            const el = Array.from(document.querySelectorAll('*')).find(e => 
                e.textContent?.trim() === key || 
                e.getAttribute('aria-label') === key
            );
            if (!el) return null;
            
            // Строим путь от корня, но останавливаемся на первом уникальном предке
            let path = [];
            let current = el;
            while (current && current.tagName !== 'BODY') {
                let tag = current.tagName.toLowerCase();
                if (current.id) {
                    path.unshift(`//${tag}[@id='${current.id}']`);
                    break;
                }
                if (current.getAttribute('data-testid')) {
                    path.unshift(`//${tag}[@data-testid='${current.getAttribute('data-testid')}']`);
                    break;
                }
                // Иначе добавляем классы, если они стабильны
                let classes = Array.from(current.classList).filter(c => 
                    !/^(css-|js-|is-)/.test(c)
                );
                if (classes.length > 0) {
                    path.unshift(`//${tag}[contains(@class, '${classes[0]}')]`);
                } else {
                    path.unshift(`//${tag}`);
                }
                current = current.parentElement;
            }
            return path.join('');
        }
        """,
        elementKey
    );
  }
}