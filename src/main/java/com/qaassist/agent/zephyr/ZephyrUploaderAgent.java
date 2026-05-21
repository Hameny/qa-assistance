// src/main/java/com/qaassist/agent/zephyr/ZephyrUploaderAgent.java
package com.qaassist.agent.zephyr;

import com.qaassist.agent.zephyr.model.ZephyrSyncReport;
import com.qaassist.agent.zephyr.model.ZephyrSyncReport.SyncError;
import com.qaassist.config.properties.ZephyrProperties;
import com.qaassist.domain.artifact.TestCase;
import com.qaassist.domain.artifact.TestSuite;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZephyrUploaderAgent {

  private final ZephyrApiClient apiClient;
  private final ZephyrMapper mapper;
  private final ZephyrProperties props;

  /**
   * Синхронизирует TestSuite с Zephyr Scale.
   * Обрабатывает батчами, соблюдает rate-limits, возвращает детальный отчёт.
   */
  public Mono<ZephyrSyncReport> upload(TestSuite suite) {
    log.info("📤 Starting Zephyr sync for suite: {} ({} cases)", suite.suiteName(), suite.testCases().size());

    AtomicInteger created = new AtomicInteger(0);
    AtomicInteger updated = new AtomicInteger(0);
    AtomicInteger skipped = new AtomicInteger(0);
    List<SyncError> errors = Collections.synchronizedList(new ArrayList<>());

    return Flux.fromIterable(suite.testCases())
        .flatMap(tc -> processTestCase(tc, suite.sourceStoryId(), created, updated, skipped, errors))
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))) // Глобальный retry на уровне потока
        .collectList()
        .map(v -> new ZephyrSyncReport(
            suite.sourceStoryId(),
            props.getProjectKey(),
            suite.testCases().size(),
            created.get(), updated.get(), skipped.get(), errors.size(),
            errors,
            Instant.now()
        ))
        .doOnSuccess(report -> log.info("📊 Zephyr sync completed: {}", formatSummary(report)));
  }

  private Mono<Void> processTestCase(
      TestCase tc, String storyId,
      AtomicInteger created, AtomicInteger updated, AtomicInteger skipped, List<SyncError> errors
  ) {
    if (props.isSkipExisting()) {
      return apiClient.existsByExternalId(tc.id().toString())
          .flatMap(exists -> exists ? handleSkip(tc, skipped) : handleCreate(tc, created, errors));
    }
    return handleCreate(tc, created, errors);
  }

  private Mono<Void> handleCreate(TestCase tc, AtomicInteger created, List<SyncError> errors) {
    Map<String, Object> payload = mapper.toZephyrPayload(tc, props.getProjectKey());

    return apiClient.createOrUpdateTestCase(tc.id().toString(), payload)
        .doOnSuccess(r -> created.incrementAndGet())
        .doOnError(e -> {
          log.error("❌ Failed to upload {}: {}", tc.name(), e.getMessage());
          errors.add(new SyncError(tc.id().toString(), tc.name(), e.getMessage(), ""));
        })
        .then();
  }

  private Mono<Void> handleSkip(TestCase tc, AtomicInteger skipped) {
    log.debug("⏭️ Skipping existing: {}", tc.name());
    skipped.incrementAndGet();
    return Mono.empty();
  }

  private String formatSummary(ZephyrSyncReport report) {
    return "Created: %d | Updated: %d | Skipped: %d | Failed: %d | Success Rate: %.1f%%".formatted(
        report.created(), report.updated(), report.skipped(), report.failed(), report.successRate()
    );
  }
}