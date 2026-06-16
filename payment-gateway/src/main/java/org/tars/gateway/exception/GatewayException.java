package org.tars.gateway.exception;

public class GatewayException extends RuntimeException {
    private final int statusCode;
    private final String errorCode;

    public GatewayException(int statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() { return statusCode; }
    public String getErrorCode() { return errorCode; }

    public static GatewayException unauthorized(String msg) { return new GatewayException(401, "UNAUTHORIZED", msg); }
    public static GatewayException forbidden(String msg) { return new GatewayException(403, "FORBIDDEN", msg); }
    public static GatewayException notFound(String msg) { return new GatewayException(404, "NOT_FOUND", msg); }
    public static GatewayException tooManyRequests(String msg) { return new GatewayException(429, "TOO_MANY_REQUESTS", msg); }
    public static GatewayException badGateway(String msg) { return new GatewayException(502, "BAD_GATEWAY", msg); }
    public static GatewayException serviceUnavailable(String msg) { return new GatewayException(503, "SERVICE_UNAVAILABLE", msg); }
}
