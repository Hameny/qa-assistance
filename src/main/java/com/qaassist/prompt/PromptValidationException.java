package com.qaassist.prompt;

import java.util.Set;

public class PromptValidationException extends RuntimeException {
  private final String templateId;
  private final Set<String> errors;

  public PromptValidationException(String templateId, Set<String> errors) {
    super("Prompt validation failed for '" + templateId + "': " + String.join("; ", errors));
    this.templateId = templateId;
    this.errors = errors;
  }

  public Set<String> getErrors() { return errors; }
}