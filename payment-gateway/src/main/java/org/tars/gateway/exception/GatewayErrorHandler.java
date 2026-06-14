package org.tars.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.context.GatewayContext;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized error handler for the gateway.
 * Produces consistent JSON error responses.
 */
public class GatewayErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GatewayErrorHandler() {}

    /**
     * Build error response body and set context accordingly.
     */
    public static byte[] handleError(GatewayContext context, GatewayException ex) {
        context.setResponseStatus(ex.getStatusCode());
        return buildErrorResponse(context, ex.getStatusCode(), ex.getErrorCode(), ex.getMessage());
    }

    public static byte[] handleError(GatewayContext context, int status, String code, String message) {
        context.setResponseStatus(status);
        return buildErrorResponse(context, status, code, message);
    }

    public static byte[] handleUnexpectedError(GatewayContext context, Throwable ex) {
        log.error("Unexpected gateway error for request {}: {}", context.getRequestId(), ex.getMessage(), ex);
        context.setResponseStatus(500);
        return buildErrorResponse(context, 500, "INTERNAL_ERROR", "Internal gateway error");
    }

    private static byte[] buildErrorResponse(GatewayContext context, int status, String code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("timestamp", Instant.now().toString());
        error.put("status", status);
        error.put("error", code);
        error.put("message", message);
        error.put("path", context.getPath());
        error.put("requestId", context.getRequestId());

        try {
            context.addResponseHeader("Content-Type", "application/json");
            return MAPPER.writeValueAsBytes(error);
        } catch (Exception e) {
            log.error("Failed to serialize error response", e);
            return ("{\"error\":\"" + code + "\",\"message\":\"" + message + "\"}").getBytes();
        }
    }
}

