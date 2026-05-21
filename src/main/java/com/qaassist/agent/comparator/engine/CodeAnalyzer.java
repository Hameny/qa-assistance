// src/main/java/com/qaassist/agent/comparator/engine/CodeAnalyzer.java
package com.qaassist.agent.comparator.engine;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CodeAnalyzer {

  /**
   * Извлекает тестовые методы и их действия из сгенерированного Java-кода.
   */
  public List<AnalyzedTest> analyze(String javaSourceCode) {
    CompilationUnit cu = new JavaParser().parse(javaSourceCode).getResult().orElseThrow();
    List<AnalyzedTest> tests = new ArrayList<>();

    cu.findAll(MethodDeclaration.class).stream()
        .filter(m -> m.getAnnotationByName("Test").isPresent())
        .forEach(m -> {
          List<AnalyzedStep> steps = new ArrayList<>();
          m.accept(new StepExtractingVisitor(), steps);

          String displayName = m.getAnnotationByName("DisplayName")
              .map(a -> ((StringLiteralExpr)a.getExpression(0)).getValue())
              .orElse(m.getNameAsString());

          tests.add(new AnalyzedTest(m.getNameAsString(), displayName, steps));
        });

    log.debug("🔍 Analyzed {} test methods from AST", tests.size());
    return tests;
  }

  public record AnalyzedTest(String methodName, String displayName, List<AnalyzedStep> steps) {}
  public record AnalyzedStep(String type, String target, String expected, String lineInfo) {}

  private static class StepExtractingVisitor extends VoidVisitorAdapter<List<AnalyzedStep>> {
    private int currentLine = 0;

    @Override
    public void visit(MethodCallExpr n, List<AnalyzedStep> steps) {
      super.visit(n, steps);
      currentLine = n.getRange().map(r -> r.begin.line).orElse(0);

      String method = n.getNameAsString();
      String target = extractFirstStringArg(n);

      if (isAssertion(method)) {
        steps.add(new AnalyzedStep("ASSERTION", target, extractExpectedValue(n), "line:" + currentLine));
      } else if (isUiAction(method) || isApiAction(method)) {
        steps.add(new AnalyzedStep("ACTION", target, method, "line:" + currentLine));
      }
    }

    private String extractFirstStringArg(MethodCallExpr call) {
      return call.getArguments().stream()
          .findFirst()
          .filter(a -> a instanceof StringLiteralExpr)
          .map(a -> ((StringLiteralExpr) a).getValue())
          .orElse("");
    }

    private String extractExpectedValue(MethodCallExpr call) {
      // Упрощённо: ищем equalTo(), statusCode(), containsText()
      return call.getNameAsString().equals("statusCode") && !call.getArguments().isEmpty()
          ? extractFirstStringArg(call) : "asserted";
    }

    private boolean isAssertion(String m) {
      return m.equals("statusCode") || m.equals("body") || m.equals("containsText") ||
          m.equals("isVisible") || m.equals("assertThat");
    }
    private boolean isUiAction(String m) { return List.of("click", "fill", "navigate", "check").contains(m); }
    private boolean isApiAction(String m) { return List.of("get", "post", "put", "delete", "body").contains(m); }
  }
}