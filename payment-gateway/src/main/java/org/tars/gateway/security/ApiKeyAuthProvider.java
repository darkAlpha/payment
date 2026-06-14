package org.tars.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.context.GatewayContext;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * API Key-based authentication provider.
 * Validates X-API-Key header against configured keys.
 */
public class ApiKeyAuthProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthProvider.class);

    private final GatewayConfig.ApiKeyConfig config;

    public ApiKeyAuthProvider(GatewayConfig.ApiKeyConfig config) {
        this.config = config;
    }

    @Override
    public String type() {
        return "API_KEY";
    }

    @Override
    public boolean supports(GatewayContext context) {
        String apiKey = context.getHeader(config.getHeaderName());
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public Optional<AuthenticationResult> authenticate(GatewayContext context) {
        String apiKey = context.getHeader(config.getHeaderName());
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        GatewayConfig.ApiKeyEntry entry = config.getKeys().get(apiKey);
        if (entry == null) {
            log.warn("Invalid API key attempted: {}...{}", 
                    apiKey.substring(0, Math.min(4, apiKey.length())), 
                    apiKey.length() > 4 ? "***" : "");
            return Optional.empty();
        }

        // Check path-level access
        if (!entry.getAllowedPaths().isEmpty()) {
            boolean pathAllowed = entry.getAllowedPaths().stream()
                    .anyMatch(pattern -> matchPath(context.getPath(), pattern));
            if (!pathAllowed) {
                log.warn("API key '{}' not authorized for path: {}", entry.getName(), context.getPath());
                return Optional.empty();
            }
        }

        Set<String> roles = new HashSet<>(entry.getRoles());
        log.debug("API key authenticated: name={}, roles={}", entry.getName(), roles);

        return Optional.of(new AuthenticationResult(
                entry.getName(),
                roles,
                "API_KEY",
                Long.MAX_VALUE // API keys don't expire via token
        ));
    }

    private boolean matchPath(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return path.startsWith(prefix) && !path.substring(prefix.length() + 1).contains("/");
        }
        return path.equals(pattern);
    }
}

