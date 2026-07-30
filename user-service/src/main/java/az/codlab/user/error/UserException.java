package az.codlab.user.error;

import az.codlab.common.exception.handling.error.ErrorCode;
import az.codlab.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UserException extends BaseException {

    public UserException(ErrorCode errorCode, HttpStatus status, String detail, Object[] args) {
        super(errorCode, status, detail, args);
    }

}
