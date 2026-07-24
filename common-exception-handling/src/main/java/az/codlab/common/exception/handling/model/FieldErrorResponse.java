package az.codlab.common.exception.handling.model;

public record FieldErrorResponse(
        String field,
        String message
) {
}
