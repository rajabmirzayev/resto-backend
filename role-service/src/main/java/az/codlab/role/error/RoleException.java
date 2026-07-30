package az.codlab.role.error;

import az.codlab.common.exception.handling.error.ErrorCode;
import az.codlab.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public class RoleException extends BaseException {

    public RoleException(ErrorCode errorCode, HttpStatus status, String detail, Object[] args) {
        super(errorCode, status, detail, args);
    }

}
