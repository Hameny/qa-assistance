// src/test/java/com/qaassist/agent/DiscrepanciesFixAgentTest.java
package com.qaassist.agent;

import com.qaassist.agent.analysis.DiscrepancyAnalyzer;
import com.qaassist.agent.fix.DiscrepancyFixEngine;
import com.qaassist.agent.report.DiscrepancyReportGenerator;
import com.qaassist.domain.artifact.TestSuite;
import com.qaassist.domain.discrepancy.Discrepancy;
import com.qaassist.domain.discrepancy.FixStrategy;
import com.qaassist.domain.requirement.UserStory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscrepanciesFixAgentTest {

  @Mock private DiscrepancyAnalyzer analyzer;
  @Mock private DiscrepancyFixEngine fixEngine;
  @Mock private DiscrepancyReportGenerator reportGenerator;
  @InjectMocks private DiscrepanciesFixAgent agent;

  @Test
  @DisplayName("Полный цикл: анализ → фикс → отчёт")
  void fullProcessFlow() {
    // Arrange
    UserStory story = mock(UserStory.class);
    TestSuite suite = mock(TestSuite.class);
    Discrepancy disc = mock(Discrepancy.class);
    FixStrategy safeFix = mock(FixStrategy.class);

    when(story.title()).thenReturn("Test Story");
    when(analyzer.analyze(any(), any())).thenReturn(List.of(disc));
    when(disc.isAutoFixable()).thenReturn(true);
    when(disc.possibleFixes()).thenReturn(List.of(safeFix));
    when(safeFix.isSafeToAutoApply()).thenReturn(true);
    when(safeFix.confidenceScore()).thenReturn(0.95);
    when(fixEngine.applyAutoFixes(any(), any()))
        .thenReturn(new DiscrepancyFixEngine.FixResult(suite, List.of(disc), List.of()));
    when(reportGenerator.generateReport(any(), any(), any(), any()))
        .thenReturn("# Report\n- Fixed: 1");

    // Act
    var result = agent.process(story, suite, "PROJ-1");

    // Assert
    assertThat(result.updatedSuite()).isSameAs(suite);
    assertThat(result.autoFixed()).hasSize(1);
    assertThat(result.requiresManualReview()).isEmpty();
    assertThat(result.markdownReport()).contains("# Report");
    assertThat(result.hasCriticalIssues()).isFalse();

    verify(analyzer).analyze(story, suite);
    verify(fixEngine).applyAutoFixes(suite, List.of(disc));
    verify(reportGenerator).generateReport("PROJ-1", List.of(disc), List.of(disc), List.of());
  }

  @Test
  @DisplayName("Критические проблемы помечаются в результате")
  void detectsCriticalIssues() {
    Discrepancy critical = new Discrepancy(
        UUID.randomUUID(), "Critical bug", "Desc",
        Discrepancy.DiscrepancyType.REQUIREMENT_CONTRADICTION,
        Discrepancy.Severity.CRITICAL,
        List.of(), null, List.of(),
        Discrepancy.ResolutionStatus.DETECTED,
        Instant.now(), null, null
    );

    var result = new DiscrepanciesFixAgent.FixAgentResult(
        mock(TestSuite.class), List.of(critical), List.of(), List.of(critical), "Report"
    );

    assertThat(result.hasCriticalIssues()).isTrue();
  }
}