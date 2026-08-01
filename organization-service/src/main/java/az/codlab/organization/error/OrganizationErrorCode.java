package az.codlab.organization.error;

import az.codlab.common.exception.handling.error.ErrorCode;
import az.codlab.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public enum OrganizationErrorCode implements ErrorCode {

    ORGANIZATION_NOT_FOUND("3001", "error.org.not-found.title", "error.org.not-found.message"),
    ORGANIZATION_SLUG_DUPLICATE("3002", "error.org.slug-duplicate.title", "error.org.slug-duplicate.message"),
    ORGANIZATION_CREATION_FAILED("3003", "error.org.creation-failed.title", "error.org.creation-failed.message"),
    ORGANIZATION_HAS_ACTIVE_ORDERS("3004", "error.org.has-active-orders.title", "error.org.has-active-orders.message"),
    ORGANIZATION_ACCESS_DENIED("3005", "error.org.access-denied.title", "error.org.access-denied.message");

    private final String code;
    private final String titleKey;
    private final String messageKey;

    OrganizationErrorCode(String code, String titleKey, String messageKey) {
        this.code = code;
        this.titleKey = titleKey;
        this.messageKey = messageKey;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String titleKey() {
        return titleKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }

    @Override
    public BaseException exception(HttpStatus status, Object... args) {
        return new OrganizationException(this, status, null, args);
    }

    @Override
    public BaseException exceptionWithMessage(HttpStatus status, String message) {
        return new OrganizationException(this, status, message, null);
    }

    public BaseException notFound() {
        return exception(HttpStatus.NOT_FOUND);
    }

    public BaseException conflict() {
        return exception(HttpStatus.CONFLICT);
    }

    public BaseException badRequest() {
        return exception(HttpStatus.BAD_REQUEST);
    }

    public BaseException internal() {
        return exception(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public BaseException forbidden() {
        return exception(HttpStatus.FORBIDDEN);
    }

}
