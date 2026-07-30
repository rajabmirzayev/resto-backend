package az.codlab.table.error;

import az.codlab.common.exception.handling.error.ErrorCode;
import az.codlab.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public enum TableErrorCode implements ErrorCode {

    TABLE_NOT_FOUND("3001", "error.table.table-not-found.title", "error.table.table-not-found.message"),
    SECTION_NOT_FOUND("3002", "error.table.section-not-found.title", "error.table.section-not-found.message"),
    TABLE_HAS_ACTIVE_ORDER("2001", "error.table.has-active-order.title", "error.table.has-active-order.message"),
    SECTION_IS_LAST("2002", "error.table.section-is-last.title", "error.table.section-is-last.message");

    private final String code;
    private final String titleKey;
    private final String messageKey;

    TableErrorCode(String code, String titleKey, String messageKey) {
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
        return new TableException(this, status, null, args);
    }

    @Override
    public BaseException exceptionWithMessage(HttpStatus status, String message) {
        return new TableException(this, status, message, null);
    }

    public BaseException notFound() {
        return exception(HttpStatus.NOT_FOUND);
    }

    public BaseException conflict() {
        return exception(HttpStatus.CONFLICT);
    }

}
