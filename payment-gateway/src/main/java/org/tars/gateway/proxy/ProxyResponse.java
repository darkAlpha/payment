package org.tars.gateway.proxy;

import java.util.Map;

public record ProxyResponse(int statusCode, Map<String, String> headers, byte[] body) {
    public static ProxyResponse error(int status, String msg) {
        String json = "{\"error\":\"UPSTREAM_ERROR\",\"message\":\"" + msg.replace("\"", "\\\"") + "\"}";
        return new ProxyResponse(status, Map.of("Content-Type", "application/json"), json.getBytes());
    }
    public boolean isSuccess() { return statusCode >= 200 && statusCode < 300; }
    public boolean isServerError() { return statusCode >= 500; }
}
