package az.flowix.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    static final int MAX_LENGTH = 30;
    static final int MIN_DIGITS = 7;
    static final int MAX_DIGITS = 15;

    @Override
    public void initialize(ValidPhone annotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            return false;
        }
        if (!trimmed.matches("[0-9+\\-(). ]+")) {
            return false;
        }
        long digits = trimmed.chars().filter(Character::isDigit).count();
        return digits >= MIN_DIGITS && digits <= MAX_DIGITS;
    }

}
