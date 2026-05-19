// src/main/java/com/qaassist/domain/validation/Traceable.java
package com.qaassist.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TraceableValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Traceable {
  String message() default "Artifact must have traceability to at least one requirement";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
  boolean required() default true;
}