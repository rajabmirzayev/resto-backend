package az.codlab.organization.handler;

import az.codlab.common.exception.handling.config.ErrorProperties;
import az.codlab.common.exception.handling.handler.AbstractGlobalExceptionHandler;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrganizationExceptionHandler extends AbstractGlobalExceptionHandler {

    public OrganizationExceptionHandler(MessageSource messageSource, ErrorProperties errorProperties) {
        super(messageSource, errorProperties);
    }

}
