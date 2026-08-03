package az.flowix.common.exception.handling.exception;

import az.flowix.common.exception.handling.error.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;
    private final String detail;
    private final Object[] args;

    protected BaseException(ErrorCode errorCode, HttpStatus status, String detail, Object[] args) {
        super(errorCode.messageKey());
        this.errorCode = errorCode;
        this.status = status;
        this.detail = detail;
        this.args = args != null ? args : new Object[0];
    }

}
