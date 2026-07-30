package az.codlab.setting.handler;

import az.codlab.common.exception.handling.config.ErrorProperties;
import az.codlab.common.exception.handling.handler.AbstractGlobalExceptionHandler;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SettingExceptionHandler extends AbstractGlobalExceptionHandler {

    public SettingExceptionHandler(MessageSource messageSource, ErrorProperties errorProperties) {
        super(messageSource, errorProperties);
    }

}
