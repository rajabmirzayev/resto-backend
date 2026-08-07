package az.flowix.setting.error;

import az.flowix.common.exception.handling.error.ErrorCode;
import az.flowix.common.exception.handling.exception.BaseException;
import org.springframework.http.HttpStatus;

public enum SettingErrorCode implements ErrorCode {

    SETTINGS_NOT_FOUND("3001", "error.setting.not-found.title", "error.setting.not-found.message");

    private final String code;
    private final String titleKey;
    private final String messageKey;

    SettingErrorCode(String code, String titleKey, String messageKey) {
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
        return new SettingException(this, status, null, args);
    }

    @Override
    public BaseException exceptionWithMessage(HttpStatus status, String message) {
        return new SettingException(this, status, message, null);
    }

    public BaseException notFound() {
        return exception(HttpStatus.NOT_FOUND);
    }

    public BaseException badRequest() {
        return exception(HttpStatus.BAD_REQUEST);
    }

}
