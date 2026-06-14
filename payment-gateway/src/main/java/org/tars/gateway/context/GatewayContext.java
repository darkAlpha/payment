package org.tars.gateway.context;

import java.time.Instant;
import java.util.*;

/**
 * Gateway request context - carries all request metadata through the filter chain.
 * Thread-safe per request lifecycle.
 */
public class GatewayContext {

    private final String requestId;
    private final Instant startTime;
    private final String method;
    private final String path;
    private final String originalPath;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final byte[] body;

    // Mutable state
    private String resolvedUpstream;
    private String authenticatedSubject;
    private Set<String> roles = new HashSet<>();
    private Map<String, Object> attributes = new HashMap<>();
    private int responseStatus = 200;
    private byte[] responseBody;
    private Map<String, String> responseHeaders = new HashMap<>();
    private boolean aborted = false;
    private String abortReason;

    public GatewayContext(String requestId, String method, String path,
                          Map<String, String> headers, Map<String, String> queryParams, byte[] body) {
        this.requestId = requestId;
        this.startTime = Instant.now();
        this.method = method;
        this.path = path;
        this.originalPath = path;
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
        this.queryParams = queryParams != null ? new HashMap<>(queryParams) : new HashMap<>();
        this.body = body;
    }

    public String getRequestId() { return requestId; }
    public Instant getStartTime() { return startTime; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getOriginalPath() { return originalPath; }
    public Map<String, String> getHeaders() { return headers; }
    public Map<String, String> getQueryParams() { return queryParams; }
    public byte[] getBody() { return body; }

    public String getHeader(String name) {
        return headers.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public String getResolvedUpstream() { return resolvedUpstream; }
    public void setResolvedUpstream(String resolvedUpstream) { this.resolvedUpstream = resolvedUpstream; }

    public String getAuthenticatedSubject() { return authenticatedSubject; }
    public void setAuthenticatedSubject(String authenticatedSubject) { this.authenticatedSubject = authenticatedSubject; }

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public void addRole(String role) { this.roles.add(role); }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) { return (T) attributes.get(key); }
    public void setAttribute(String key, Object value) { attributes.put(key, value); }

    public int getResponseStatus() { return responseStatus; }
    public void setResponseStatus(int responseStatus) { this.responseStatus = responseStatus; }

    public byte[] getResponseBody() { return responseBody; }
    public void setResponseBody(byte[] responseBody) { this.responseBody = responseBody; }

    public Map<String, String> getResponseHeaders() { return responseHeaders; }
    public void addResponseHeader(String key, String value) { responseHeaders.put(key, value); }

    public boolean isAborted() { return aborted; }
    public String getAbortReason() { return abortReason; }

    public void abort(int status, String reason) {
        this.aborted = true;
        this.responseStatus = status;
        this.abortReason = reason;
    }

    public long getElapsedMs() {
        return java.time.Duration.between(startTime, Instant.now()).toMillis();
    }
}

