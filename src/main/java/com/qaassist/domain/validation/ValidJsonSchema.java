// src/main/java/com/qaassist/domain/validation/ValidJsonSchema.java
package com.qaassist.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = JsonSchemaValidatorImpl.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidJsonSchema {
  String message() default "JSON не соответствует схеме";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
  String schemaPath() default ""; // например: "schemas/user-story.schema.json"
}