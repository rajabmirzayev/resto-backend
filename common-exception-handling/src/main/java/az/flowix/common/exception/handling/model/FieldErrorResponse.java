package az.flowix.common.exception.handling.model;

public record FieldErrorResponse(
        String field,
        String message
) {
}
