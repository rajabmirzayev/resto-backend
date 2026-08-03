package az.flowix.common.exception.handling.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValidEnumValidator implements ConstraintValidator<ValidEnum, String> {

    private Set<String> allowedValues;
    private boolean ignoreCase;
    private String enumClassName;

    @Override
    public void initialize(ValidEnum annotation) {
        this.ignoreCase = annotation.ignoreCase();
        this.enumClassName = annotation.enumClass().getSimpleName();
        var enumConstants = annotation.enumClass().getEnumConstants();
        this.allowedValues = Stream.of(enumConstants)
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (ignoreCase) {
            return allowedValues.stream().anyMatch(v -> v.equalsIgnoreCase(value));
        }
        return allowedValues.contains(value);
    }

}
