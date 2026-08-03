package az.codlab.waiter.handler;

import az.codlab.common.exception.handling.config.ErrorProperties;
import az.codlab.common.exception.handling.handler.AbstractGlobalExceptionHandler;
import az.codlab.waiter.error.WaiterErrorCode;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WaiterExceptionHandler extends AbstractGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WaiterExceptionHandler.class);

    public WaiterExceptionHandler(MessageSource messageSource, ErrorProperties errorProperties) {
        super(messageSource, errorProperties);
    }

    @ExceptionHandler(FeignException.class)
    public ProblemDetail handleFeignTransportFailure(FeignException ex, HttpServletRequest request) {
        log.warn("Upstream transport failure on {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        return handleBaseException(WaiterErrorCode.UPSTREAM_UNAVAILABLE.serviceUnavailable(), request);
    }

}
