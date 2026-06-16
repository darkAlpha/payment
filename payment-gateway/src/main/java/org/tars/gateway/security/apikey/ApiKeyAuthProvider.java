package org.tars.gateway.security.apikey;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tars.gateway.config.GatewayProperties;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.security.AuthenticationResult;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * API Key authentication provider.
 * Validates X-API-Key header with per-key roles and path restrictions.
 */
@Slf4j
@Component
public class ApiKeyAuthProvider {

    private final GatewayProperties.ApiKey config;

    public ApiKeyAuthProvider(GatewayProperties properties) {
        this.config = properties.getSecurity().getApiKey();
    }

    public Optional<AuthenticationResult> authenticate(GatewayContext context) {
        if (!config.isEnabled()) return Optional.empty();

        String apiKey = context.getHeader(config.getHeaderName());
        if (apiKey == null || apiKey.isBlank()) return Optional.empty();

        GatewayProperties.ApiKeyEntry entry = config.getKeys().get(apiKey);
        if (entry == null) {
            log.warn("[{}] Invalid API key", context.getRequestId());
            return Optional.empty();
        }

        // Path restriction check
        if (!entry.getAllowedPaths().isEmpty()) {
            boolean allowed = entry.getAllowedPaths().stream()
                    .anyMatch(p -> matchPath(context.getPath(), p));
            if (!allowed) {
                log.warn("[{}] API key '{}' not allowed for path: {}", context.getRequestId(), entry.getName(), context.getPath());
                return Optional.empty();
            }
        }

        Set<String> roles = new HashSet<>(entry.getRoles());
        return Optional.of(new AuthenticationResult(entry.getName(), roles, "API_KEY"));
    }

    private boolean matchPath(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            return path.startsWith(pattern.substring(0, pattern.length() - 3));
        }
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return path.startsWith(prefix) && !path.substring(prefix.length() + 1).contains("/");
        }
        return path.equals(pattern);
    }
}

