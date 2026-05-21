// src/main/java/com/qaassist/api/PipelineController.java
package com.qaassist.api;

import com.qaassist.pipeline.PipelineExecution;
import com.qaassist.pipeline.PipelineOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/pipeline")
@RequiredArgsConstructor
public class PipelineController {

  private final PipelineOrchestrator orchestrator;
  private final Map<UUID, PipelineExecution> executions = new ConcurrentHashMap<>(); // В продакшене → Redis/PostgreSQL

  @PostMapping("/execute")
  public ResponseEntity<Map<String, String>> execute(@RequestBody ExecuteRequest request) {
    var ctx = PipelineExecution.start(request.projectId(), request.issueKey());
    executions.put(ctx.getPipelineId(), ctx);

    orchestrator.execute(request.projectId(), request.issueKey())
        .thenAccept(executions::put);

    return ResponseEntity.accepted().body(Map.of("pipelineId", ctx.getPipelineId().toString()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<PipelineExecution> status(@PathVariable UUID id) {
    return executions.containsKey(id)
        ? ResponseEntity.ok(executions.get(id))
        : ResponseEntity.notFound().build();
  }

  public record ExecuteRequest(String projectId, String issueKey) {}
}