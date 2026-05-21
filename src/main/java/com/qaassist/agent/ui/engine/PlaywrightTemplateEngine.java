// src/main/java/com/qaassist/agent/ui/engine/PlaywrightTemplateEngine.java
package com.qaassist.agent.ui.engine;

import com.qaassist.agent.ui.model.UiTestStructure;
import com.qaassist.agent.ui.model.UiTestStructure.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
public class PlaywrightTemplateEngine {

  /**
   * Генерирует два файла: Page Object + Test Class.
   */
  public GeneratedUiTest generate(UiTestStructure structure) {
    String pageCode = generatePageObject(structure);
    String testCode = generateTestClass(structure);

    return new GeneratedUiTest(
        structure.pageClassName(), pageCode,
        structure.testClassName(), testCode
    );
  }

  private String generatePageObject(UiTestStructure s) {
    return """
            package %s.pages;
            
            import com.microsoft.playwright.Page;
            import com.microsoft.playwright.Locator;
            
            public class %s {
                private final Page page;
                
            %s
                public %s(Page page) {
                    this.page = page;
                }
            %s
            }
            """.formatted(
        s.packageName(),
        s.pageClassName(),
        generatePageFields(s.pageFields()),
        s.pageClassName(),
        generatePageMethods(s.pageFields())
    );
  }

  private String generatePageFields(List<PageField> fields) {
    return fields.stream().map(f -> """
                private final Locator %s;
                
                // %s
            """.formatted(f.fieldName(), f.description())).collect(Collectors.joining("\n"));
  }

  private String generatePageMethods(List<PageField> fields) {
    return fields.stream().map(f -> {
      String locatorExpr = switch (f.locatorStrategy()) {
        case "getbyTestId" -> "page.getByTestId(\"%s\")".formatted(f.locatorValue());
        case "getByRole" -> "page.getByRole(AriaRole.%s, new Page.GetByRoleOptions().setName(\"%s\"))"
            .formatted(f.fieldType(), f.locatorValue());
        default -> "page.locator(\"%s\")".formatted(f.locatorValue());
      };

      String assign = "this.%s = %s;".formatted(f.fieldName(), locatorExpr);

      return switch (f.fieldType()) {
        case "INPUT" -> """
                        public void enter%s(String value) {
                            %s.fill(value);
                        }
                        public void clear%s() {
                            %s.clear();
                        }
                    """.formatted(capitalize(f.fieldName()), f.fieldName(), capitalize(f.fieldName()), f.fieldName());
        case "BUTTON" -> """
                        public void click%s() {
                            %s.click();
                        }
                    """.formatted(capitalize(f.fieldName()), f.fieldName());
        default -> """
                        public Locator get%s() {
                            return %s;
                        }
                    """.formatted(capitalize(f.fieldName()), f.fieldName());
      };
    }).collect(Collectors.joining("\n"));
  }

  private String generateTestClass(UiTestStructure s) {
    return """
            package %s.tests;
            
            %s
            import com.microsoft.playwright.Page;
            import com.microsoft.playwright.options.AriaRole;
            import com.qaassist.pages.%s;
            import org.junit.jupiter.api.*;
            import org.junit.jupiter.api.extension.ExtendWith;
            import com.microsoft.playwright.junit.UsePlaywright;
            import static org.assertj.core.api.Assertions.assertThat;
            
            @UsePlaywright
            @Tag("ui")
            @DisplayName("%s")
            public class %s {
            
                @Test
                @Order(%d)
                @DisplayName("%s")
                %s
                void %s(Page page) {
            %s                %s loginPage = new %s(page);
            %s                page.navigate("%s");
            %s            }
            }
            """.formatted(
        s.packageName(),
        String.join("\n", s.imports().values()),
        s.pageClassName(),
        s.testClassName().replace("Test", ""),
        s.testClassName(),
        s.testMethods().get(0).priority(),
        s.testMethods().get(0).description(),
        s.testMethods().get(0).tags().stream().map(t -> "@Tag(\"%s\")".formatted(t)).collect(Collectors.joining("\n                ")),
        s.testMethods().get(0).methodName(),
        s.pageClassName().toLowerCase(),
        s.pageClassName(),
        s.pageClassName(),
        generateTestActions(s.testMethods().get(0).actions(), s.pageClassName().toLowerCase()),
        s.baseUrl()
    );
  }

  private String generateTestActions(List<StepAction> actions, String pageVar) {
    return actions.stream().map(a -> switch (a.type()) {
      case "FILL" -> "%s.enter%s(\"%s\");".formatted(pageVar, capitalize(a.targetField()), a.inputValue());
      case "CLICK" -> "%s.click%s();".formatted(pageVar, capitalize(a.targetField()));
      case "ASSERT_VISIBLE" -> "assertThat(%s.get%s()).isVisible();".formatted(pageVar, capitalize(a.targetField()));
      case "ASSERT_TEXT" -> "assertThat(%s.get%s()).containsText(\"%s\");".formatted(pageVar, capitalize(a.targetField()), a.assertion());
      default -> "// Action: %s".formatted(a.type());
    }).collect(Collectors.joining("\n            "));
  }

  private String capitalize(String s) {
    return s == null || s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  public record GeneratedUiTest(
      String pageClassName, String pageSource,
      String testClassName, String testSource
  ) {}
}