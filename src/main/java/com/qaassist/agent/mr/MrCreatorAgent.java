// src/main/java/com/qaassist/agent/mr/MrCreatorAgent.java
package com.qaassist.agent.mr;

import com.qaassist.agent.api.ApiTestGenerator;
import com.qaassist.agent.api.ApiTestGenerator.GeneratedApiTest;
import com.qaassist.agent.ui.UiAutomationSkill;
import com.qaassist.agent.mr.model.CommitFile;
import com.qaassist.agent.mr.model.MrCreationResult;
import com.qaassist.config.properties.GitlabProperties;
import com.qaassist.domain.artifact.TestSuite;
import com.qaassist.integration.gitlab.GitLabApiClient;
import com.qaassist.integration.gitlab.GitLabApiClient.GitlabMrResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MrCreatorAgent {

  private final GitLabApiClient gitlabClient;
  private final TestProjectPackager packager;
  private final MrDescriptionGenerator descGenerator;
  private final GitlabProperties props;

  /**
   * Полный цикл: упаковка → ветка → коммит → MR.
   */
  public MrCreationResult create(TestSuite suite,
      List<GeneratedApiTest> apiTests,
      List<UiAutomationSkill.GeneratedUiTest> uiTests) {
    log.info("🔀 Starting MR creation for suite: {}", suite.suiteName());

    try {
      // 1. Упаковка проекта
      List<CommitFile> files = packager.packageProject(suite, apiTests, uiTests);
      String branchName = "qa-assist/auto-tests-%s-%d".formatted(
          suite.suiteName().toLowerCase().replaceAll("[^a-z0-9]", "-"),
          System.currentTimeMillis() / 1000);

      // 2. Проверка/создание ветки
      if (!gitlabClient.branchExists(props.getProjectId(), branchName)) {
        gitlabClient.createBranch(props.getProjectId(), branchName, props.getDefaultBranch());
      }

      // 3. Проверка существующего MR
      Optional<GitlabMrResponse> existing = gitlabClient.findExistingMr(props.getProjectId(), branchName);
      if (existing.isPresent()) {
        log.info("⏭️ MR already exists: {}", existing.get().webUrl());
        return new MrCreationResult(branchName, existing.get().webUrl(), existing.get().iid(), true, null, files.size());
      }

      // 4. Коммит файлов
      gitlabClient.commitFiles(props.getProjectId(), branchName, files,
          "feat(qa-assist): auto-generate tests for %s".formatted(suite.suiteName()));

      // 5. Создание MR
      String title = "🤖 QA Assist: Auto-tests for %s".formatted(suite.suiteName());
      String description = descGenerator.generate(suite, ""); // можно передать JSON метрик
      GitlabMrResponse mr = gitlabClient.createMergeRequest(
          props.getProjectId(), branchName, props.getDefaultBranch(), title, description);

      log.info("✅ MR created successfully: {}", mr.webUrl());
      return new MrCreationResult(branchName, mr.webUrl(), mr.iid(), true, null, files.size());

    } catch (Exception e) {
      log.error("❌ Failed to create MR: {}", e.getMessage(), e);
      return MrCreationResult.failure("qa-assist-failed", e.getMessage());
    }
  }
}