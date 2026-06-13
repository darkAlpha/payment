package org.tars.core.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Global exception handler with i18n message resolution.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        String traceId = UUID.randomUUID().toString();
        String message = messageSource.getMessage(
                ex.getErrorCode(), ex.getArgs(), ex.getErrorCode(), LocaleContextHolder.getLocale());

        log.warn("Business error [{}]: {} - {}", traceId, ex.getErrorCode(), message);

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(message)
                .traceId(traceId)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("Unexpected error [{}]: {}", traceId, ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ErrorCode.INTERNAL_ERROR)
                .message("Internal server error")
                .traceId(traceId)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
