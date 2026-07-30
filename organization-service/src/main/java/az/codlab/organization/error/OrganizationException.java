package az.codlab.organization.error;

import az.codlab.common.exception.handling.error.ErrorCode;
import az.codlab.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public class OrganizationException extends BaseException {

    public OrganizationException(ErrorCode errorCode, HttpStatus status, String detail, Object[] args) {
        super(errorCode, status, detail, args);
    }

}
