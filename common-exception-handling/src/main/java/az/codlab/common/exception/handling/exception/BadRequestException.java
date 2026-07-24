package az.codlab.common.exception.handling.exception;

import az.codlab.common.exception.handling.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class BadRequestException extends BaseException {

    public BadRequestException(ErrorCode errorCode, Object[] args) {
        super(errorCode, HttpStatus.BAD_REQUEST, null, args);
    }

    public BadRequestException(ErrorCode errorCode, HttpStatus status, Object[] args) {
        super(errorCode, status, null, args);
    }

    public BadRequestException(ErrorCode errorCode, HttpStatus status, String detail, Object[] args) {
        super(errorCode, status, detail, args);
    }

}
