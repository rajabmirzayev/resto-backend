package az.codlab.common.exception.handling.handler;

import az.codlab.common.exception.handling.config.ErrorProperties;
import az.codlab.common.exception.handling.decoder.FeignClientException;
import az.codlab.common.exception.handling.error.CommonErrorCode;
import az.codlab.common.exception.handling.exception.BaseException;
import az.codlab.common.exception.handling.model.FieldErrorResponse;
import az.codlab.common.exception.handling.model.TraceHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

public abstract class AbstractGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AbstractGlobalExceptionHandler.class);

    protected final MessageSource messageSource;
    protected final ErrorProperties errorProperties;

    protected AbstractGlobalExceptionHandler(MessageSource messageSource, ErrorProperties errorProperties) {
        this.messageSource = messageSource;
        this.errorProperties = errorProperties;
    }

    private String buildErrorKey(String code) {
        return errorProperties.getServiceKey() + "_" + code;
    }

    private Locale resolveLocale(HttpServletRequest request) {
        Locale locale = request.getLocale();
        return locale != null ? locale : Locale.ENGLISH;
    }

    private URI resolveInstance(HttpServletRequest request) {
        String traceId = firstNonNull(
                request.getHeader(TraceHeaders.TRACEPARENT),
                request.getHeader(TraceHeaders.X_B3_TRACE_ID),
                request.getHeader(TraceHeaders.X_TRACE_ID),
                request.getHeader(TraceHeaders.X_B3_SPAN_ID)
        );
        return traceId != null
                ? URI.create("trace:" + traceId) : URI.create(request.getRequestURI());
    }

    private String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @ExceptionHandler(BaseException.class)
    public ProblemDetail handleBaseException(BaseException ex, HttpServletRequest request) {
        Locale locale = resolveLocale(request);

        String title = messageSource.getMessage(
                ex.getErrorCode().titleKey(), null, ex.getErrorCode().titleKey(), locale
        );
        String key = buildErrorKey(ex.getErrorCode().code());

        if (ex instanceof FeignClientException fce) {
            title = fce.getSourceTitle() != null ? fce.getSourceTitle() : title;
            key = fce.getSourceKey() != null ? fce.getSourceKey() : key;
        }

        String detail;

        if (ex.getDetail() != null) {
            detail = ex.getDetail();
        } else {
            detail = messageSource.getMessage(
                    ex.getErrorCode().messageKey(), ex.getArgs(), ex.getErrorCode().messageKey(), locale
            );
        }

        ProblemDetail pd = ProblemDetail.forStatus(ex.getStatus());
        pd.setType(URI.create("about:blank"));
        pd.setTitle(title);
        pd.setDetail(detail);
        pd.setInstance(resolveInstance(request));
        pd.setProperty("key", key);
        pd.setProperty("path", request.getRequestURI());
        pd.setProperty("timestamp", Instant.now());

        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Locale locale = resolveLocale(request);

        List<FieldErrorResponse> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new FieldErrorResponse(f.getField(),
                        messageSource.getMessage(f, locale)))
                .toList();

        ProblemDetail pd = ProblemDetail.forStatus(400);
        pd.setType(URI.create("about:blank"));
        pd.setTitle(messageSource.getMessage(CommonErrorCode.VALIDATION_ERROR.titleKey(), null,
                CommonErrorCode.VALIDATION_ERROR.titleKey(), locale));
        pd.setDetail(messageSource.getMessage(CommonErrorCode.VALIDATION_ERROR.messageKey(), null,
                CommonErrorCode.VALIDATION_ERROR.messageKey(), locale));
        pd.setInstance(resolveInstance(request));
        pd.setProperty("key", buildErrorKey(CommonErrorCode.VALIDATION_ERROR.code()));
        pd.setProperty("path", request.getRequestURI());
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("fieldErrors", fields);

        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Locale locale = resolveLocale(request);

        ProblemDetail pd = ProblemDetail.forStatus(400);
        pd.setType(URI.create("about:blank"));
        pd.setTitle(messageSource.getMessage(CommonErrorCode.CONSTRAINT_VIOLATION.titleKey(), null,
                CommonErrorCode.CONSTRAINT_VIOLATION.titleKey(), locale));
        pd.setDetail(messageSource.getMessage(CommonErrorCode.CONSTRAINT_VIOLATION.messageKey(), null,
                CommonErrorCode.CONSTRAINT_VIOLATION.messageKey(), locale));
        pd.setInstance(resolveInstance(request));
        pd.setProperty("key", buildErrorKey(CommonErrorCode.CONSTRAINT_VIOLATION.code()));
        pd.setProperty("path", request.getRequestURI());
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleJsonParse(HttpMessageNotReadableException ex, HttpServletRequest request) {
        Locale locale = resolveLocale(request);

        String detail = messageSource.getMessage(CommonErrorCode.JSON_PARSE_ERROR.messageKey(), null,
                CommonErrorCode.JSON_PARSE_ERROR.messageKey(), locale);
        Throwable cause = ex.getMostSpecificCause();
        if (cause.getMessage() != null) {
            detail = cause.getMessage();
        }

        ProblemDetail pd = ProblemDetail.forStatus(400);
        pd.setType(URI.create("about:blank"));
        pd.setTitle(messageSource.getMessage(CommonErrorCode.JSON_PARSE_ERROR.titleKey(), null,
                CommonErrorCode.JSON_PARSE_ERROR.titleKey(), locale));
        pd.setDetail(detail);
        pd.setInstance(resolveInstance(request));
        pd.setProperty("key", buildErrorKey(CommonErrorCode.JSON_PARSE_ERROR.code()));
        pd.setProperty("path", request.getRequestURI());
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ProblemDetail handleRequestParamError(Exception ex, HttpServletRequest request) {
        Locale locale = resolveLocale(request);

        Object[] args = null;
        if (ex instanceof MethodArgumentTypeMismatchException mae) {
            args = new Object[]{mae.getName(), mae.getValue()};
        } else if (ex instanceof MissingServletRequestParameterException msr) {
            args = new Object[]{msr.getParameterName()};
        }

        ProblemDetail pd = ProblemDetail.forStatus(400);
        pd.setType(URI.create("about:blank"));
        pd.setTitle(messageSource.getMessage(CommonErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH.titleKey(), null,
                CommonErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH.titleKey(), locale));
        pd.setDetail(
                messageSource.getMessage(CommonErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH.messageKey(), args,
                        CommonErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH.messageKey(), locale));
        pd.setInstance(resolveInstance(request));
        pd.setProperty("key", buildErrorKey(CommonErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH.code()));
        pd.setProperty("path", request.getRequestURI());
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                  HttpServletRequest request) {
        Locale locale = resolveLocale(request);

        ProblemDetail pd = ProblemDetail.forStatus(405);
        pd.setType(URI.create("about:blank"));
        pd.setTitle(messageSource.getMessage(CommonErrorCode.METHOD_NOT_ALLOWED.titleKey(), null,
                CommonErrorCode.METHOD_NOT_ALLOWED.titleKey(), locale));
        pd.setDetail(messageSource.getMessage(CommonErrorCode.METHOD_NOT_ALLOWED.messageKey(), null,
                CommonErrorCode.METHOD_NOT_ALLOWED.messageKey(), locale));
        pd.setInstance(resolveInstance(request));
        pd.setProperty("key", buildErrorKey(CommonErrorCode.METHOD_NOT_ALLOWED.code()));
        pd.setProperty("path", request.getRequestURI());
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        Locale locale = resolveLocale(request);

        ProblemDetail pd = ProblemDetail.forStatus(400);
        pd.setType(URI.create("about:blank"));
        pd.setTitle(messageSource.getMessage(CommonErrorCode.VALIDATION_ERROR.titleKey(), null,
                CommonErrorCode.VALIDATION_ERROR.titleKey(), locale));
        pd.setDetail(ex.getMessage());
        pd.setInstance(resolveInstance(request));
        pd.setProperty("key", buildErrorKey(CommonErrorCode.VALIDATION_ERROR.code()));
        pd.setProperty("path", request.getRequestURI());
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        Locale locale = resolveLocale(request);

        ProblemDetail pd = ProblemDetail.forStatus(500);
        pd.setType(URI.create("about:blank"));
        pd.setTitle(messageSource.getMessage(CommonErrorCode.INTERNAL_ERROR.titleKey(), null,
                CommonErrorCode.INTERNAL_ERROR.titleKey(), locale));
        pd.setDetail(messageSource.getMessage(CommonErrorCode.INTERNAL_ERROR.messageKey(), null,
                CommonErrorCode.INTERNAL_ERROR.messageKey(), locale));
        pd.setInstance(resolveInstance(request));
        pd.setProperty("key", buildErrorKey(CommonErrorCode.INTERNAL_ERROR.code()));
        pd.setProperty("path", request.getRequestURI());
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ProblemDetail handleAuthorizationDenied(AuthorizationDeniedException ex, HttpServletRequest request) {
        Locale locale = resolveLocale(request);

        ProblemDetail pd = ProblemDetail.forStatus(403);
        pd.setType(URI.create("about:blank"));
        pd.setTitle(messageSource.getMessage(CommonErrorCode.ACCESS_DENIED.titleKey(), null,
                CommonErrorCode.ACCESS_DENIED.titleKey(), locale));
        pd.setDetail(messageSource.getMessage(CommonErrorCode.ACCESS_DENIED.messageKey(), null,
                CommonErrorCode.ACCESS_DENIED.messageKey(), locale));
        pd.setInstance(resolveInstance(request));
        pd.setProperty("key", buildErrorKey(CommonErrorCode.ACCESS_DENIED.code()));
        pd.setProperty("path", request.getRequestURI());
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

}
