// src/main/java/com/qaassist/domain/task/TaskStatus.java
package com.qaassist.domain.task;

/**
 * Статус обработки задачи в пайплайне QA Assistance.
 */
public enum TaskStatus {
  TODO("To Do", 0),
  IN_PROGRESS("In Progress", 1),
  IN_REVIEW("In Review", 2),
  AUTO_TESTING("Auto-Testing", 3),
  COMPLETED("Completed", 4),
  BLOCKED("Blocked", 5);

  private final String label;
  private final int workflowOrder;

  TaskStatus(String label, int workflowOrder) {
    this.label = label;
    this.workflowOrder = workflowOrder;
  }

  public String label() { return label; }
  public int workflowOrder() { return workflowOrder; }

  public static TaskStatus fromJiraStatus(String jiraStatus) {
    if (jiraStatus == null) return TODO;
    return switch (jiraStatus.toLowerCase()) {
      case "done", "resolved", "closed" -> COMPLETED;
      case "in progress", "testing" -> IN_PROGRESS;
      case "in review", "ready for qa" -> IN_REVIEW;
      case "blocked" -> BLOCKED;
      default -> TODO;
    };
  }
}