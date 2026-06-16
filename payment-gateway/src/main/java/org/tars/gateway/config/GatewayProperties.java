package org.tars.gateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.*;

/**
 * Type-safe gateway configuration properties.
 * Bound from application.yaml under 'gateway' prefix.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    @Valid
    private Server server = new Server();

    @Valid
    private Security security = new Security();

    @Valid
    private List<Route> routes = new ArrayList<>();

    @Valid
    private FeatureFlags featureFlags = new FeatureFlags();

    @Valid
    private RateLimit rateLimit = new RateLimit();

    @Valid
    private Metrics metrics = new Metrics();

    @Data
    public static class Server {
        @Min(1)
        private int port = 8080;
        private int bossThreads = 1;
        private int workerThreads = 0;
        private int maxContentLength = 10 * 1024 * 1024;
        private int idleTimeoutSeconds = 60;
        private boolean accessLogEnabled = true;
    }

    @Data
    public static class Security {
        @Valid
        private Jwt jwt = new Jwt();
        @Valid
        private ApiKey apiKey = new ApiKey();
        @Valid
        private Cors cors = new Cors();
        private List<String> publicPaths = new ArrayList<>();
    }

    @Data
    public static class Jwt {
        private boolean enabled = true;
        @NotBlank
        private String secret = "change-me-in-production-256-bit-minimum-secret-key-for-hs256";
        private String issuer = "payment-gateway";
        private long expirationMs = 3_600_000;
        private String headerName = "Authorization";
        private String tokenPrefix = "Bearer ";
    }

    @Data
    public static class ApiKey {
        private boolean enabled = true;
        private String headerName = "X-API-Key";
        private Map<String, ApiKeyEntry> keys = new LinkedHashMap<>();
    }

    @Data
    public static class ApiKeyEntry {
        private String name;
        private List<String> roles = new ArrayList<>();
        private List<String> allowedPaths = new ArrayList<>();
        private int rateLimit = 1000;
    }

    @Data
    public static class Cors {
        private boolean enabled = true;
        private List<String> allowedOrigins = List.of("*");
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        private List<String> allowedHeaders = List.of("*");
        private boolean allowCredentials = false;
        private int maxAge = 3600;
    }

    @Data
    public static class Route {
        @NotBlank
        private String id;
        @NotBlank
        private String path;
        private String method = "*";
        private List<Upstream> upstreams = new ArrayList<>();
        private String loadBalancer = "round-robin";
        private List<String> requiredRoles = new ArrayList<>();
        private int timeoutMs = 30_000;
        private int retries = 3;
        private boolean stripPrefix = false;
        private Map<String, String> addHeaders = new LinkedHashMap<>();
        private Versioning versioning;
        private CircuitBreakerConfig circuitBreaker;
    }

    @Data
    public static class Upstream {
        @NotBlank
        private String url;
        private int weight = 1;
        private boolean healthy = true;
        private String version;
    }

    @Data
    public static class Versioning {
        private String strategy = "header";
        private String headerName = "X-API-Version";
        private Map<String, Integer> percentages = new LinkedHashMap<>();
    }

    @Data
    public static class CircuitBreakerConfig {
        private int failureRateThreshold = 50;
        private int waitDurationInOpenStateMs = 60_000;
        private int slidingWindowSize = 10;
    }

    @Data
    public static class FeatureFlags {
        private boolean enabled = true;
        private Map<String, FeatureEntry> flags = new LinkedHashMap<>();
    }

    @Data
    public static class FeatureEntry {
        private boolean enabled = true;
        private String description = "";
        private List<String> allowedRoles = new ArrayList<>();
        private int percentage = 100;
    }

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private int defaultRpm = 100;
        private Map<String, Integer> pathLimits = new LinkedHashMap<>();
    }

    @Data
    public static class Metrics {
        private boolean enabled = true;
        private String path = "/gateway/metrics";
    }
}

