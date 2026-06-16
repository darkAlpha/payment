package org.tars.gateway.context;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Immutable request context carrying all state through the filter pipeline.
 * Each request gets its own context instance.
 */
public class GatewayContext {

    private final String requestId;
    private final Instant startTime;
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final byte[] body;

    // Mutable routing state
    private String resolvedUpstream;
    private String authenticatedSubject;
    private Set<String> roles = new HashSet<>();
    private final Map<String, Object> attributes = new HashMap<>();
    private int responseStatus = 200;
    private byte[] responseBody;
    private final Map<String, String> responseHeaders = new LinkedHashMap<>();
    private boolean aborted;
    private String abortReason;

    public GatewayContext(String requestId, String method, String path,
                          Map<String, String> headers, Map<String, String> queryParams, byte[] body) {
        this.requestId = requestId;
        this.startTime = Instant.now();
        this.method = method;
        this.path = path;
        this.headers = new LinkedHashMap<>(headers != null ? headers : Map.of());
        this.queryParams = new LinkedHashMap<>(queryParams != null ? queryParams : Map.of());
        this.body = body;
    }

    // --- Getters ---
    public String getRequestId() { return requestId; }
    public Instant getStartTime() { return startTime; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public Map<String, String> getHeaders() { return headers; }
    public Map<String, String> getQueryParams() { return queryParams; }
    public byte[] getBody() { return body; }

    public String getHeader(String name) {
        return headers.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    // --- Routing ---
    public String getResolvedUpstream() { return resolvedUpstream; }
    public void setResolvedUpstream(String url) { this.resolvedUpstream = url; }

    // --- Security ---
    public String getAuthenticatedSubject() { return authenticatedSubject; }
    public void setAuthenticatedSubject(String subject) { this.authenticatedSubject = subject; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }

    // --- Attributes ---
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) { return (T) attributes.get(key); }
    public void setAttribute(String key, Object value) { attributes.put(key, value); }

    // --- Response ---
    public int getResponseStatus() { return responseStatus; }
    public void setResponseStatus(int status) { this.responseStatus = status; }
    public byte[] getResponseBody() { return responseBody; }
    public void setResponseBody(byte[] body) { this.responseBody = body; }
    public Map<String, String> getResponseHeaders() { return responseHeaders; }
    public void addResponseHeader(String key, String value) { responseHeaders.put(key, value); }

    // --- Abort ---
    public boolean isAborted() { return aborted; }
    public String getAbortReason() { return abortReason; }

    public void abort(int status, String reason) {
        this.aborted = true;
        this.responseStatus = status;
        this.abortReason = reason;
    }

    public long getElapsedMs() {
        return Duration.between(startTime, Instant.now()).toMillis();
    }
}

