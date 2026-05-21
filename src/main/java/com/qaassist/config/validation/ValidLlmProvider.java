package com.qaassist.config.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidLlmProviderValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLlmProvider {
  String message() default "Invalid LLM configuration for the selected provider";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}