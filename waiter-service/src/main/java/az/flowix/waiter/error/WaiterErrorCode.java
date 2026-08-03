package az.flowix.waiter.error;

import az.flowix.common.exception.handling.error.ErrorCode;
import az.flowix.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public enum WaiterErrorCode implements ErrorCode {

    ACCESS_DENIED("3003", "error.waiter.access-denied.title", "error.waiter.access-denied.message"),
    UPSTREAM_UNAVAILABLE("9001", "error.waiter.upstream-unavailable.title", "error.waiter.upstream-unavailable.message"),
    UPSTREAM_ERROR("9002", "error.waiter.upstream-error.title", "error.waiter.upstream-error.message");

    private final String code;
    private final String titleKey;
    private final String messageKey;

    WaiterErrorCode(String code, String titleKey, String messageKey) {
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
        return new WaiterException(this, status, null, args);
    }

    @Override
    public BaseException exceptionWithMessage(HttpStatus status, String message) {
        return new WaiterException(this, status, message, null);
    }

    public BaseException forbidden() {
        return exception(HttpStatus.FORBIDDEN);
    }

    public BaseException badGateway() {
        return exception(HttpStatus.BAD_GATEWAY);
    }

    public BaseException serviceUnavailable() {
        return exception(HttpStatus.SERVICE_UNAVAILABLE);
    }

}
