package az.flowix.order.error;

import az.flowix.common.exception.handling.error.ErrorCode;
import az.flowix.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public class OrderException extends BaseException {

    public OrderException(ErrorCode errorCode, HttpStatus status, String detail, Object[] args) {
        super(errorCode, status, detail, args);
    }

}
