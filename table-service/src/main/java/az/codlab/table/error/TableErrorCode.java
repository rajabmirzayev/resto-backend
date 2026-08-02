package az.codlab.table.error;

import az.codlab.common.exception.handling.error.ErrorCode;
import az.codlab.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public enum TableErrorCode implements ErrorCode {

    TABLE_NOT_FOUND("3001", "error.table.table-not-found.title", "error.table.table-not-found.message"),
    SECTION_NOT_FOUND("3002", "error.table.section-not-found.title", "error.table.section-not-found.message"),
    ACCESS_DENIED("3003", "error.table.access-denied.title", "error.table.access-denied.message"),
    TABLE_NUMBER_TAKEN("3004", "error.table.table-number-taken.title", "error.table.table-number-taken.message"),
    SECTION_NAME_TAKEN("3005", "error.table.section-name-taken.title", "error.table.section-name-taken.message"),
    TABLE_HAS_ACTIVE_ORDER("2001", "error.table.has-active-order.title", "error.table.has-active-order.message"),
    SECTION_IS_LAST("2002", "error.table.section-is-last.title", "error.table.section-is-last.message"),
    INVALID_STATUS_TRANSITION("2003", "error.table.invalid-status-transition.title", "error.table.invalid-status-transition.message"),
    TABLE_IS_OCCUPIED("2004", "error.table.table-is-occupied.title", "error.table.table-is-occupied.message"),
    TABLE_HAS_RESERVATION("2005", "error.table.table-has-reservation.title", "error.table.table-has-reservation.message"),
    CONFLICT("2006", "error.table.conflict.title", "error.table.conflict.message"),
    INVALID_STATUS("4001", "error.table.invalid-status.title", "error.table.invalid-status.message"),
    RESERVATION_EXCEEDS_CAPACITY("4002", "error.table.reservation-exceeds-capacity.title", "error.table.reservation-exceeds-capacity.message"),
    ORDER_ID_REQUIRED("4003", "error.table.order-id-required.title", "error.table.order-id-required.message");

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

    public BaseException forbidden() {
        return exception(HttpStatus.FORBIDDEN);
    }

    public BaseException conflict() {
        return exception(HttpStatus.CONFLICT);
    }

    public BaseException badRequest() {
        return exception(HttpStatus.BAD_REQUEST);
    }

}
