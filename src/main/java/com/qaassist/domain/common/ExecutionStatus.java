// src/main/java/com/qaassist/domain/common/ExecutionStatus.java
package com.qaassist.domain.common;

public enum ExecutionStatus {
  NOT_STARTED,
  IN_PROGRESS,
  WAITING_APPROVAL,  // для auto-mode: ожидание подтверждения инженера
  COMPLETED,
  FAILED,
  PARTIALLY_COMPLETED // некоторые агенты выполнились, другие — нет
}