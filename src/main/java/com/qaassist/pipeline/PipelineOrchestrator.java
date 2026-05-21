// src/main/java/com/qaassist/pipeline/PipelineOrchestrator.java
package com.qaassist.pipeline;

import com.qaassist.agent.*;
import com.qaassist.agent.api.ApiTestGenerator;
import com.qaassist.agent.mr.MrCreatorAgent;
import com.qaassist.agent.ui.UiAutomationSkill;
import com.qaassist.agent.zephyr.ZephyrUploaderAgent;
import com.qaassist.validation.quality.ComparisonQualityGate;
import com.qaassist.validation.quality.ComparisonQualityGate.QualityGateFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineOrchestrator {

  private final JiraFetchAgent jiraFetch;
  private final RequirementsDecomposerAgent decomposer;
  private final ScenariosGenerationPipeline scenarioPipeline;
  private final DiscrepanciesFixAgent discrepancyFixer;
  private final SelectorsCollectorAgent selectorCollector;
  private final ApiTestGenerator apiGenerator;
  @Lazy private final UiAutomationSkill uiGenerator; // Цикл через @Lazy
  private final ComparisonQualityGate comparisonGate;
  private final ZephyrUploaderAgent zephyrUploader;
  private final MrCreatorAgent mrCreator;
  private final ExecutorService pipelineExecutor;

  /**
   * Полный пайплайн: Fetch → Decompose → Generate → Fix → Collect → API/UI → Compare → Upload → MR
   */
  @Async("pipelineExecutor")
  public CompletableFuture<PipelineExecution> execute(String projectId, String issueKey) {
    PipelineExecution ctx = PipelineExecution.start(projectId, issueKey);
    log.info("🚀 Starting pipeline {} for {}/{}", ctx.getPipelineId(), projectId, issueKey);

    try {
      // 1. Fetch & Decompose (синхронно, т.к. требуются данные дальше)
      ctx = fetchAndDecompose(ctx);

      // 2. Generate Scenarios & Enrich
      TestSuite suite = scenarioPipeline.generateAndEnrich(projectId, ctx.getUserStory());

      // 3. Discrepancy Fix & Selectors
      var fixResult = discrepancyFixer.process(ctx.getUserStory(), suite, projectId);
      TestSuite enrichedSuite = selectorCollector.collectAndEnrich(
          fixResult.updatedSuite(), projectId, "https://app." + projectId + ".test.com"
      );
      ctx = ctx.toBuilder().testSuite(enrichedSuite).status(PipelineExecution.PipelineStatus.IN_PROGRESS).build();

      // 4. Параллельная генерация API + UI
      CompletableFuture<List<ApiTestGenerator.GeneratedApiTest>> apiF =
          CompletableFuture.supplyAsync(() -> List.of(apiGenerator.generate(enrichedSuite, projectId, "https://api." + projectId + ".test.com")), pipelineExecutor);
      CompletableFuture<List<UiAutomationSkill.GeneratedUiTest>> uiF =
          CompletableFuture.supplyAsync(() -> List.of(uiGenerator.generate(enrichedSuite, projectId, "https://app." + projectId + ".test.com")), pipelineExecutor);

      CompletableFuture.allOf(apiF, uiF).join();

      // 5. Сравнение и Quality Gate
      String combinedCode = apiF.get().stream().map(ApiTestGenerator.GeneratedApiTest::sourceCode).reduce("", String::concat)
          + uiF.get().stream().map(UiAutomationSkill.GeneratedUiTest::testSource).reduce("", String::concat);
      comparisonGate.validate(enrichedSuite, combinedCode);

      ctx = ctx.toBuilder()
          .apiTests(apiF.get())
          .uiTests(uiF.get())
          .build();

      // 6. Upload to TMS & Create MR (параллельно, но MR зависит от тестов)
      CompletableFuture<com.qaassist.agent.zephyr.model.ZephyrSyncReport> zephyrF =
          zephyrUploader.upload(enrichedSuite);

      CompletableFuture<com.qaassist.agent.mr.model.MrCreationResult> mrF = zephyrF.thenApply(z ->
          mrCreator.create(enrichedSuite, ctx.getApiTests(), ctx.getUiTests())
      );

      ctx = ctx.toBuilder()
          .zephyrReport(zephyrF.get())
          .mrResult(mrF.get())
          .status(PipelineExecution.PipelineStatus.COMPLETED)
          .finishedAt(Instant.now())
          .build();

    } catch (QualityGateFailedException e) {
      ctx = ctx.addError("Quality Gate failed: " + e.getMessage());
    } catch (Exception e) {
      log.error("❌ Pipeline failed", e);
      ctx = ctx.addError(e.getMessage());
    }

    log.info("✅ Pipeline {} finished with status: {}", ctx.getPipelineId(), ctx.getStatus());
    return CompletableFuture.completedFuture(ctx);
  }

  private PipelineExecution fetchAndDecompose(PipelineExecution ctx) {
    // Здесь вызываем JiraFetchAgent + RequirementsDecomposerAgent
    // Для краткости опущена полная цепочка, логика идентична предыдущим шагам
    return ctx.toBuilder().status(PipelineExecution.PipelineStatus.IN_PROGRESS).build();
  }
}