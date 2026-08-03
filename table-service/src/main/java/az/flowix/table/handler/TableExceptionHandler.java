package az.flowix.table.handler;

import az.flowix.common.exception.handling.config.ErrorProperties;
import az.flowix.common.exception.handling.handler.AbstractGlobalExceptionHandler;
import az.flowix.table.error.TableErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TableExceptionHandler extends AbstractGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TableExceptionHandler.class);

    public TableExceptionHandler(MessageSource messageSource, ErrorProperties errorProperties) {
        super(messageSource, errorProperties);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                      HttpServletRequest request) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage() : ex.getMessage();

        if (message != null) {
            if (message.contains("uq_sections_org_name")) {
                return handleBaseException(TableErrorCode.SECTION_NAME_TAKEN.conflict(), request);
            }
            if (message.contains("uq_tables_org_number")) {
                return handleBaseException(TableErrorCode.TABLE_NUMBER_TAKEN.conflict(), request);
            }
        }
        log.error("Unhandled data integrity violation on {} {}", request.getMethod(),
                request.getRequestURI(), ex);
        return handleBaseException(TableErrorCode.CONFLICT.conflict(), request);
    }

}
