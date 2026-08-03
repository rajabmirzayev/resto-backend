package az.flowix.auth.handler;

import az.flowix.common.exception.handling.config.ErrorProperties;
import az.flowix.common.exception.handling.handler.AbstractGlobalExceptionHandler;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler extends AbstractGlobalExceptionHandler {

    public AuthExceptionHandler(MessageSource messageSource, ErrorProperties errorProperties) {
        super(messageSource, errorProperties);
    }

}
