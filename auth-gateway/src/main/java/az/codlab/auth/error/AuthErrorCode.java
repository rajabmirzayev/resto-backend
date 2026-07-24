package az.codlab.auth.error;

import az.codlab.common.exception.handling.error.ErrorCode;
import az.codlab.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {

    INVALID_CREDENTIALS("001", "error.auth.invalid-credentials.title", "error.auth.invalid-credentials.message"),
    TOKEN_EXPIRED("002", "error.auth.token-expired.title", "error.auth.token-expired.message"),
    INVALID_TOKEN("003", "error.auth.invalid-token.title", "error.auth.invalid-token.message"),
    LOGOUT_FAILED("004", "error.auth.logout-failed.title", "error.auth.logout-failed.message"),
    KEYCLOAK_UNAVAILABLE("005", "error.auth.keycloak-unavailable.title", "error.auth.keycloak-unavailable.message");

    private final String code;
    private final String titleKey;
    private final String messageKey;

    AuthErrorCode(String code, String titleKey, String messageKey) {
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
        return new AuthException(this, status, null, args);
    }

    @Override
    public BaseException exceptionWithMessage(HttpStatus status, String message) {
        return new AuthException(this, status, message, null);
    }

    public BaseException exception() {
        return switch (this) {
            case INVALID_CREDENTIALS -> exception(HttpStatus.UNAUTHORIZED);
            case TOKEN_EXPIRED -> exception(HttpStatus.UNAUTHORIZED);
            case INVALID_TOKEN -> exception(HttpStatus.UNAUTHORIZED);
            case LOGOUT_FAILED -> exception(HttpStatus.BAD_GATEWAY);
            case KEYCLOAK_UNAVAILABLE -> exception(HttpStatus.BAD_GATEWAY);
        };
    }
}
