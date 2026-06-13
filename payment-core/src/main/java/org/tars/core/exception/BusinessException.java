package org.tars.core.exception;

import lombok.Getter;

/**
 * Base business exception with i18n support.
 * Error codes map to messages_xx.properties files.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    public BusinessException(String errorCode, Object... args) {
        super(errorCode);
        this.errorCode = errorCode;
        this.args = args;
    }

    public BusinessException(String errorCode, Throwable cause, Object... args) {
        super(errorCode, cause);
        this.errorCode = errorCode;
        this.args = args;
    }
}
