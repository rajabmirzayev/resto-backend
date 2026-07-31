package az.codlab.common.exception.handling.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ValidEnumValidator.class)
public @interface ValidEnum {

    String message() default "Invalid value. Must be one of: {enumClass}";

    Class<? extends Enum<?>> enumClass();

    boolean ignoreCase() default true;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
