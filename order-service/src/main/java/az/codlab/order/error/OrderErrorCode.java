package az.codlab.order.error;

import az.codlab.common.exception.handling.error.ErrorCode;
import az.codlab.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND("3001", "error.order.not-found.title", "error.order.not-found.message"),
    INVALID_STATUS_TRANSITION("4001", "error.order.invalid-status.title", "error.order.invalid-status.message"),
    CANNOT_CANCEL_COMPLETED("4002", "error.order.cancel-completed.title", "error.order.cancel-completed.message"),
    CANNOT_CANCEL_PAID("4003", "error.order.cancel-paid.title", "error.order.cancel-paid.message"),
    ORDER_NOT_PENDING("4004", "error.order.not-pending.title", "error.order.not-pending.message"),
    ORDER_NOT_ACTIVE("4005", "error.order.not-active.title", "error.order.not-active.message"),
    ITEM_NOT_FOUND("4006", "error.order.item-not-found.title", "error.order.item-not-found.message"),
    INVALID_ITEM_STATUS("4007", "error.order.invalid-item-status.title", "error.order.invalid-item-status.message"),
    PAYMENT_ALREADY_COMPLETED("4008", "error.order.payment-completed.title", "error.order.payment-completed.message"),
    ORDER_NOT_CANCELLABLE("4009", "error.order.not-cancellable.title", "error.order.not-cancellable.message"),
    TABLE_NOT_FOUND("4010", "error.order.table-not-found.title", "error.order.table-not-found.message"),
    TABLE_NOT_AVAILABLE("4011", "error.order.table-not-available.title", "error.order.table-not-available.message"),
    MENU_ITEM_NOT_FOUND("4012", "error.order.menu-item-not-found.title", "error.order.menu-item-not-found.message"),
    MENU_ITEM_NOT_AVAILABLE("4013", "error.order.menu-item-not-available.title", "error.order.menu-item-not-available.message");

    private final String code;
    private final String titleKey;
    private final String messageKey;

    OrderErrorCode(String code, String titleKey, String messageKey) {
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
        return new OrderException(this, status, null, args);
    }

    @Override
    public BaseException exceptionWithMessage(HttpStatus status, String message) {
        return new OrderException(this, status, message, null);
    }

    public BaseException notFound() {
        return exception(HttpStatus.NOT_FOUND);
    }

    public BaseException conflict() {
        return exception(HttpStatus.CONFLICT);
    }

    public BaseException badRequest() {
        return exception(HttpStatus.BAD_REQUEST);
    }

}
