package az.codlab.menu.error;

import az.codlab.common.exception.handling.error.ErrorCode;
import az.codlab.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public enum MenuErrorCode implements ErrorCode {

    CATEGORY_NOT_FOUND("3001", "error.menu.category-not-found.title", "error.menu.category-not-found.message"),
    ITEM_NOT_FOUND("3002", "error.menu.item-not-found.title", "error.menu.item-not-found.message");

    private final String code;
    private final String titleKey;
    private final String messageKey;

    MenuErrorCode(String code, String titleKey, String messageKey) {
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
        return new MenuException(this, status, null, args);
    }

    @Override
    public BaseException exceptionWithMessage(HttpStatus status, String message) {
        return new MenuException(this, status, message, null);
    }

    public BaseException notFound() {
        return exception(HttpStatus.NOT_FOUND);
    }

}
