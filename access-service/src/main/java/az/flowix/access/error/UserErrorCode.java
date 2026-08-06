package az.flowix.access.error;

import az.flowix.common.exception.handling.error.ErrorCode;
import az.flowix.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND("3001", "error.user.not-found.title", "error.user.not-found.message"),
    USERNAME_DUPLICATE("3002", "error.user.username-duplicate.title", "error.user.username-duplicate.message"),
    KEYCLOAK_UNAVAILABLE("3003", "error.user.keycloak-unavailable.title", "error.user.keycloak-unavailable.message"),
    USER_ORG_MISMATCH("4004", "error.user.org-mismatch.title", "error.user.org-mismatch.message");

    private final String code;
    private final String titleKey;
    private final String messageKey;

    UserErrorCode(String code, String titleKey, String messageKey) {
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
        return new UserException(this, status, null, args);
    }

    @Override
    public BaseException exceptionWithMessage(HttpStatus status, String message) {
        return new UserException(this, status, message, null);
    }

    public BaseException notFound() {
        return exception(HttpStatus.NOT_FOUND);
    }

    public BaseException conflict() {
        return exception(HttpStatus.CONFLICT);
    }

    public BaseException forbidden() {
        return exception(HttpStatus.FORBIDDEN);
    }

}
