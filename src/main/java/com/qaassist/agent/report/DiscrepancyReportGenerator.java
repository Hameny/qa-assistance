// src/main/java/com/qaassist/agent/report/DiscrepancyReportGenerator.java
package com.qaassist.agent.report;

import com.qaassist.domain.discrepancy.Discrepancy;
import com.qaassist.domain.discrepancy.FixStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class DiscrepancyReportGenerator {

  private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  /**
   * Генерирует Markdown-отчёт для QA-инженера.
   */
  public String generateReport(String projectId, List<Discrepancy> all, List<Discrepancy> fixed, List<Discrepancy> pending) {
    StringBuilder md = new StringBuilder();
    md.append("# 🔍 Discrepancy Report — Project: `%s`\n\n".formatted(projectId));
    md.append("Generated: %s\n\n".formatted(java.time.LocalDateTime.now().format(DF)));

    // Сводка
    md.append("## 📊 Summary\n");
    md.append("| Status | Count |\n|--------|-------|\n");
    md.append("| ✅ Auto-fixed | %d |\n".formatted(fixed.size()));
    md.append("| ⚠️ Requires review | %d |\n".formatted(pending.size()));
    md.append("| 🔴 Critical | %d |\n\n".formatted(
        pending.stream().filter(d -> d.severity() == Discrepancy.Severity.CRITICAL).count()));

    // Авто-фиксы
    if (!fixed.isEmpty()) {
      md.append("## ✅ Auto-Fixed Issues\n\n");
      for (Discrepancy d : fixed) {
        md.append("### %s [`%s`]\n".formatted(d.title(), d.id()));
        md.append("- **Type**: %s | **Severity**: %s\n".formatted(d.type(), d.severity()));
        md.append("- **Description**: %s\n".formatted(d.description()));
        md.append("- **Fix applied**: %s\n\n".formatted(
            d.possibleFixes().stream().findFirst().map(FixStrategy::name).orElse("N/A")));
      }
    }

    // Требующие внимания
    if (!pending.isEmpty()) {
      md.append("## ⚠️ Requires Manual Review\n\n");
      for (Discrepancy d : pending) {
        md.append("### %s [`%s`] — %s\n".formatted(d.title(), d.id(), d.severity()));
        md.append("```diff\n");
        md.append("- %s\n".formatted(d.description()));
        md.append("```\n");
        md.append("**Sources**:\n");
        d.sources().forEach(s ->
            md.append("- `%s`: %s\n".formatted(s.type(), s.reference())));
        md.append("\n**Suggested action**: %s\n\n".formatted(
            d.possibleFixes().stream().findFirst()
                .map(f -> f.description() + " (confidence: %.0f%%)".formatted(f.confidenceScore() * 100))
                .orElse("Manual investigation required")));
      }
    }

    return md.toString();
  }
}