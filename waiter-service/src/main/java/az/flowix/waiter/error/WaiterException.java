package az.flowix.waiter.error;

import az.flowix.common.exception.handling.error.ErrorCode;
import az.flowix.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public class WaiterException extends BaseException {

    public WaiterException(ErrorCode errorCode, HttpStatus status, String detail, Object[] args) {
        super(errorCode, status, detail, args);
    }

}
