package org.tars.gateway.exception;

/**
 * Base gateway exception for all gateway-related errors.
 */
public class GatewayException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;

    public GatewayException(int statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public GatewayException(int statusCode, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() { return statusCode; }
    public String getErrorCode() { return errorCode; }

    // Factory methods
    public static GatewayException unauthorized(String message) {
        return new GatewayException(401, "UNAUTHORIZED", message);
    }

    public static GatewayException forbidden(String message) {
        return new GatewayException(403, "FORBIDDEN", message);
    }

    public static GatewayException notFound(String message) {
        return new GatewayException(404, "NOT_FOUND", message);
    }

    public static GatewayException tooManyRequests(String message) {
        return new GatewayException(429, "TOO_MANY_REQUESTS", message);
    }

    public static GatewayException badGateway(String message) {
        return new GatewayException(502, "BAD_GATEWAY", message);
    }

    public static GatewayException serviceUnavailable(String message) {
        return new GatewayException(503, "SERVICE_UNAVAILABLE", message);
    }

    public static GatewayException gatewayTimeout(String message) {
        return new GatewayException(504, "GATEWAY_TIMEOUT", message);
    }
}

