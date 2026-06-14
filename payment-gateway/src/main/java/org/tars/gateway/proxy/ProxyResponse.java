package org.tars.gateway.proxy;
import java.util.Map;
/**
 * Proxy response from upstream service.
 */
public record ProxyResponse(
        int statusCode,
        Map<String, String> headers,
        byte[] body
) {
    public static ProxyResponse error(int status, String message) {
        String json = "{\"error\":\"UPSTREAM_ERROR\",\"message\":\"" + escapeJson(message) + "\"}";
        return new ProxyResponse(status, Map.of("Content-Type", "application/json"), json.getBytes());
    }
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
    public boolean isServerError() {
        return statusCode >= 500;
    }
    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
