// src/main/java/com/qaassist/domain/common/TestType.java
package com.qaassist.domain.common;

public enum TestType {
  FUNCTIONAL,      // Бизнес-логика
  REGRESSION,      // Проверка после изменений
  SMOKE,           // Быстрая проверка критического пути
  INTEGRATION,     // Взаимодействие компонентов
  E2E,            // Полный пользовательский сценарий
  PERFORMANCE,     // Нагрузочное тестирование
  SECURITY,        // Проверки безопасности
  ACCESSIBILITY    // a11y проверки
}