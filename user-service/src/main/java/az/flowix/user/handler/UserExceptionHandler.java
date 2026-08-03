package az.flowix.user.handler;

import az.flowix.common.exception.handling.config.ErrorProperties;
import az.flowix.common.exception.handling.handler.AbstractGlobalExceptionHandler;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserExceptionHandler extends AbstractGlobalExceptionHandler {

    public UserExceptionHandler(MessageSource messageSource, ErrorProperties errorProperties) {
        super(messageSource, errorProperties);
    }

}
