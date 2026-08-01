package az.codlab.common.validation;

import az.codlab.common.type.LocalizedString;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LocalizedStringValidator implements ConstraintValidator<ValidLocalizedString, LocalizedString> {

    private Set<String> required;
    private int maxLength;

    @Override
    public void initialize(ValidLocalizedString annotation) {
        required = new LinkedHashSet<>(Arrays.asList(annotation.required()));
        maxLength = annotation.maxLength();
    }

    @Override
    public boolean isValid(LocalizedString value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        boolean valid = true;
        for (String locale : required) {
            if (isBlank(valueFor(value, locale))) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                                "Value must be provided for locale '" + locale + "'")
                        .addConstraintViolation();
                valid = false;
            }
        }
        for (String locale : new String[]{"az", "en", "ru"}) {
            String v = valueFor(value, locale);
            if (v != null && v.length() > maxLength) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                                "Value for locale '" + locale + "' must not exceed " + maxLength + " characters")
                        .addConstraintViolation();
                valid = false;
            }
            if (v != null && v.indexOf('\u0000') >= 0) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                                "Value for locale '" + locale + "' must not contain null characters")
                        .addConstraintViolation();
                valid = false;
            }
        }
        return valid;
    }

    private String valueFor(LocalizedString value, String locale) {
        return switch (locale) {
            case "az" -> value.getAz();
            case "en" -> value.getEn();
            case "ru" -> value.getRu();
            default -> null;
        };
    }

    private boolean isBlank(String v) {
        return v == null || v.isBlank();
    }
}
