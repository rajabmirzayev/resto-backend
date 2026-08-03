package az.flowix.menu.error;

import az.flowix.common.exception.handling.error.ErrorCode;
import az.flowix.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public class MenuException extends BaseException {

    public MenuException(ErrorCode errorCode, HttpStatus status, String detail, Object[] args) {
        super(errorCode, status, detail, args);
    }

}
