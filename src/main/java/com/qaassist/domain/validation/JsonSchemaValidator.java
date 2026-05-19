// src/main/java/com/qaassist/domain/validation/TraceableValidator.java
package com.qaassist.domain.validation;

import com.qaassist.domain.artifact.TestArtifact;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TraceableValidator implements ConstraintValidator<Traceable, TestArtifact> {

  private boolean required;

  @Override
  public void initialize(Traceable constraintAnnotation) {
    this.required = constraintAnnotation.required();
  }

  @Override
  public boolean isValid(TestArtifact artifact, ConstraintValidatorContext context) {
    if (artifact == null) return !required;

    var requirements = artifact.linkedRequirements();
    if (requirements == null || requirements.isEmpty()) {
      if (!required) return true;

      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(
          "Test artifact must be linked to at least one requirement for traceability"
      ).addConstraintViolation();
      return false;
    }
    return true;
  }
}