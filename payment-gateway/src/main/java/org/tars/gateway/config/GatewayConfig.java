package org.tars.gateway.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Root gateway configuration model.
 * Deserialized from gateway.yaml.
 */
public class GatewayConfig {

    private ServerConfig server = new ServerConfig();
    private SecurityConfig security = new SecurityConfig();
    private List<RouteConfig> routes = new ArrayList<>();
    private FeatureFlagConfig featureFlags = new FeatureFlagConfig();
    private RateLimitConfig rateLimit = new RateLimitConfig();
    private MetricsConfig metrics = new MetricsConfig();

    public ServerConfig getServer() { return server; }
    public void setServer(ServerConfig server) { this.server = server; }
    public SecurityConfig getSecurity() { return security; }
    public void setSecurity(SecurityConfig security) { this.security = security; }
    public List<RouteConfig> getRoutes() { return routes; }
    public void setRoutes(List<RouteConfig> routes) { this.routes = routes; }
    public FeatureFlagConfig getFeatureFlags() { return featureFlags; }
    public void setFeatureFlags(FeatureFlagConfig featureFlags) { this.featureFlags = featureFlags; }
    public RateLimitConfig getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimitConfig rateLimit) { this.rateLimit = rateLimit; }
    public MetricsConfig getMetrics() { return metrics; }
    public void setMetrics(MetricsConfig metrics) { this.metrics = metrics; }

    // --- Nested Config Classes ---

    public static class ServerConfig {
        private int port = 8080;
        private int bossThreads = 1;
        private int workerThreads = 0; // 0 = Netty default (cores * 2)
        private int maxContentLength = 10 * 1024 * 1024; // 10MB
        private int idleTimeoutSeconds = 60;
        private boolean enableAccessLog = true;

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public int getBossThreads() { return bossThreads; }
        public void setBossThreads(int bossThreads) { this.bossThreads = bossThreads; }
        public int getWorkerThreads() { return workerThreads; }
        public void setWorkerThreads(int workerThreads) { this.workerThreads = workerThreads; }
        public int getMaxContentLength() { return maxContentLength; }
        public void setMaxContentLength(int maxContentLength) { this.maxContentLength = maxContentLength; }
        public int getIdleTimeoutSeconds() { return idleTimeoutSeconds; }
        public void setIdleTimeoutSeconds(int idleTimeoutSeconds) { this.idleTimeoutSeconds = idleTimeoutSeconds; }
        public boolean isEnableAccessLog() { return enableAccessLog; }
        public void setEnableAccessLog(boolean enableAccessLog) { this.enableAccessLog = enableAccessLog; }
    }

    public static class SecurityConfig {
        private JwtConfig jwt = new JwtConfig();
        private ApiKeyConfig apiKey = new ApiKeyConfig();
        private CorsConfig cors = new CorsConfig();
        private List<String> publicPaths = new ArrayList<>();

        public JwtConfig getJwt() { return jwt; }
        public void setJwt(JwtConfig jwt) { this.jwt = jwt; }
        public ApiKeyConfig getApiKey() { return apiKey; }
        public void setApiKey(ApiKeyConfig apiKey) { this.apiKey = apiKey; }
        public CorsConfig getCors() { return cors; }
        public void setCors(CorsConfig cors) { this.cors = cors; }
        public List<String> getPublicPaths() { return publicPaths; }
        public void setPublicPaths(List<String> publicPaths) { this.publicPaths = publicPaths; }
    }

    public static class JwtConfig {
        private boolean enabled = true;
        private String secret = "change-me-in-production-at-least-256-bits-long-secret-key";
        private String issuer = "payment-gateway";
        private long expirationMs = 3600000; // 1 hour
        private String headerName = "Authorization";
        private String tokenPrefix = "Bearer ";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public long getExpirationMs() { return expirationMs; }
        public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
        public String getHeaderName() { return headerName; }
        public void setHeaderName(String headerName) { this.headerName = headerName; }
        public String getTokenPrefix() { return tokenPrefix; }
        public void setTokenPrefix(String tokenPrefix) { this.tokenPrefix = tokenPrefix; }
    }

    public static class ApiKeyConfig {
        private boolean enabled = true;
        private String headerName = "X-API-Key";
        private Map<String, ApiKeyEntry> keys = new HashMap<>();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getHeaderName() { return headerName; }
        public void setHeaderName(String headerName) { this.headerName = headerName; }
        public Map<String, ApiKeyEntry> getKeys() { return keys; }
        public void setKeys(Map<String, ApiKeyEntry> keys) { this.keys = keys; }
    }

    public static class ApiKeyEntry {
        private String name;
        private List<String> roles = new ArrayList<>();
        private List<String> allowedPaths = new ArrayList<>();
        private int rateLimit = 1000; // requests per minute

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public List<String> getAllowedPaths() { return allowedPaths; }
        public void setAllowedPaths(List<String> allowedPaths) { this.allowedPaths = allowedPaths; }
        public int getRateLimit() { return rateLimit; }
        public void setRateLimit(int rateLimit) { this.rateLimit = rateLimit; }
    }

    public static class CorsConfig {
        private boolean enabled = true;
        private List<String> allowedOrigins = List.of("*");
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        private List<String> allowedHeaders = List.of("*");
        private boolean allowCredentials = false;
        private int maxAge = 3600;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
        public List<String> getAllowedMethods() { return allowedMethods; }
        public void setAllowedMethods(List<String> allowedMethods) { this.allowedMethods = allowedMethods; }
        public List<String> getAllowedHeaders() { return allowedHeaders; }
        public void setAllowedHeaders(List<String> allowedHeaders) { this.allowedHeaders = allowedHeaders; }
        public boolean isAllowCredentials() { return allowCredentials; }
        public void setAllowCredentials(boolean allowCredentials) { this.allowCredentials = allowCredentials; }
        public int getMaxAge() { return maxAge; }
        public void setMaxAge(int maxAge) { this.maxAge = maxAge; }
    }

    public static class RouteConfig {
        private String id;
        private String path;
        private String method = "*"; // GET, POST, * etc.
        private List<UpstreamConfig> upstreams = new ArrayList<>();
        private String loadBalancer = "round-robin"; // round-robin, weighted, random, least-connections
        private List<String> requiredRoles = new ArrayList<>();
        private String version;
        private VersioningConfig versioning;
        private int timeoutMs = 30000;
        private int retries = 3;
        private boolean stripPrefix = false;
        private Map<String, String> addHeaders = new HashMap<>();
        private List<String> filters = new ArrayList<>();
        private CircuitBreakerConfig circuitBreaker;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public List<UpstreamConfig> getUpstreams() { return upstreams; }
        public void setUpstreams(List<UpstreamConfig> upstreams) { this.upstreams = upstreams; }
        public String getLoadBalancer() { return loadBalancer; }
        public void setLoadBalancer(String loadBalancer) { this.loadBalancer = loadBalancer; }
        public List<String> getRequiredRoles() { return requiredRoles; }
        public void setRequiredRoles(List<String> requiredRoles) { this.requiredRoles = requiredRoles; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public VersioningConfig getVersioning() { return versioning; }
        public void setVersioning(VersioningConfig versioning) { this.versioning = versioning; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
        public int getRetries() { return retries; }
        public void setRetries(int retries) { this.retries = retries; }
        public boolean isStripPrefix() { return stripPrefix; }
        public void setStripPrefix(boolean stripPrefix) { this.stripPrefix = stripPrefix; }
        public Map<String, String> getAddHeaders() { return addHeaders; }
        public void setAddHeaders(Map<String, String> addHeaders) { this.addHeaders = addHeaders; }
        public List<String> getFilters() { return filters; }
        public void setFilters(List<String> filters) { this.filters = filters; }
        public CircuitBreakerConfig getCircuitBreaker() { return circuitBreaker; }
        public void setCircuitBreaker(CircuitBreakerConfig circuitBreaker) { this.circuitBreaker = circuitBreaker; }
    }

    public static class UpstreamConfig {
        private String url;
        private int weight = 1;
        private boolean healthy = true;
        private String version;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public int getWeight() { return weight; }
        public void setWeight(int weight) { this.weight = weight; }
        public boolean isHealthy() { return healthy; }
        public void setHealthy(boolean healthy) { this.healthy = healthy; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }

    public static class VersioningConfig {
        private String strategy = "header"; // header, percentage
        private String headerName = "X-API-Version";
        private Map<String, Integer> percentages = new HashMap<>(); // version -> percentage

        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        public String getHeaderName() { return headerName; }
        public void setHeaderName(String headerName) { this.headerName = headerName; }
        public Map<String, Integer> getPercentages() { return percentages; }
        public void setPercentages(Map<String, Integer> percentages) { this.percentages = percentages; }
    }

    public static class CircuitBreakerConfig {
        private int failureRateThreshold = 50;
        private int waitDurationInOpenState = 60000;
        private int slidingWindowSize = 10;

        public int getFailureRateThreshold() { return failureRateThreshold; }
        public void setFailureRateThreshold(int failureRateThreshold) { this.failureRateThreshold = failureRateThreshold; }
        public int getWaitDurationInOpenState() { return waitDurationInOpenState; }
        public void setWaitDurationInOpenState(int waitDurationInOpenState) { this.waitDurationInOpenState = waitDurationInOpenState; }
        public int getSlidingWindowSize() { return slidingWindowSize; }
        public void setSlidingWindowSize(int slidingWindowSize) { this.slidingWindowSize = slidingWindowSize; }
    }

    public static class FeatureFlagConfig {
        private boolean enabled = true;
        private Map<String, FeatureEntry> flags = new HashMap<>();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Map<String, FeatureEntry> getFlags() { return flags; }
        public void setFlags(Map<String, FeatureEntry> flags) { this.flags = flags; }
    }

    public static class FeatureEntry {
        private boolean enabled = true;
        private String description;
        private List<String> allowedRoles = new ArrayList<>();
        private int percentage = 100; // rollout percentage

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getAllowedRoles() { return allowedRoles; }
        public void setAllowedRoles(List<String> allowedRoles) { this.allowedRoles = allowedRoles; }
        public int getPercentage() { return percentage; }
        public void setPercentage(int percentage) { this.percentage = percentage; }
    }

    public static class RateLimitConfig {
        private boolean enabled = true;
        private int defaultRpm = 100; // requests per minute
        private Map<String, Integer> pathLimits = new HashMap<>();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getDefaultRpm() { return defaultRpm; }
        public void setDefaultRpm(int defaultRpm) { this.defaultRpm = defaultRpm; }
        public Map<String, Integer> getPathLimits() { return pathLimits; }
        public void setPathLimits(Map<String, Integer> pathLimits) { this.pathLimits = pathLimits; }
    }

    public static class MetricsConfig {
        private boolean enabled = true;
        private String path = "/gateway/metrics";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }
}

