package az.codlab.role.error;

import az.codlab.common.exception.handling.error.ErrorCode;
import az.codlab.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public enum RoleErrorCode implements ErrorCode {

    ROLE_NOT_FOUND("3001", "error.role.not-found.title", "error.role.not-found.message"),
    ROLE_IS_SYSTEM("4003", "error.role.is-system.title", "error.role.is-system.message");

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

    public BaseException forbidden() {
        return exception(HttpStatus.FORBIDDEN);
    }

}
