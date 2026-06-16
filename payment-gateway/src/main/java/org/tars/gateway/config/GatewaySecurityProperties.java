package org.tars.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.*;

@Data
@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {
    private Jwt jwt = new Jwt();
    private ApiKey apiKey = new ApiKey();
    private List<String> publicPaths = new ArrayList<>();

    @Data
    public static class Jwt {
        private boolean enabled = true;
        private String secret = "change-me-in-production-at-least-256-bits-long-secret-key-here";
        private String issuer = "payment-gateway";
        private long expirationMs = 3_600_000;
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
    }
}
