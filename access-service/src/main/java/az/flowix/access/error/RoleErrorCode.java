package az.flowix.access.error;

import az.flowix.common.exception.handling.error.ErrorCode;
import az.flowix.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public enum RoleErrorCode implements ErrorCode {

    ROLE_NOT_FOUND("3001", "error.role.not-found.title", "error.role.not-found.message"),
    ROLE_IS_SYSTEM("4005", "error.role.is-system.title", "error.role.is-system.message"),
    ROLE_CODE_DUPLICATE("3002", "error.role.code-duplicate.title", "error.role.code-duplicate.message"),
    ROLE_ORG_MISMATCH("4003", "error.role.org-mismatch.title", "error.role.org-mismatch.message"),
    PERMISSION_NOT_FOUND("3004", "error.role.permission-not-found.title", "error.role.permission-not-found.message");

    private final String code;
    private final String titleKey;
    private final String messageKey;

    RoleErrorCode(String code, String titleKey, String messageKey) {
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
        return new RoleException(this, status, null, args);
    }

    @Override
    public BaseException exceptionWithMessage(HttpStatus status, String message) {
        return new RoleException(this, status, message, null);
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
