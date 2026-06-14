package org.tars.gateway.filter.pre;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;
import org.tars.gateway.security.ApiKeyAuthProvider;
import org.tars.gateway.security.AuthenticationProvider;
import org.tars.gateway.security.AuthenticationResult;
import org.tars.gateway.security.JwtAuthProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Authentication filter - validates JWT or API key credentials.
 * Populates context with authenticated subject and roles.
 */
public class AuthenticationFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final List<AuthenticationProvider> providers = new ArrayList<>();
    private final List<String> publicPaths;

    public AuthenticationFilter(GatewayConfig.SecurityConfig securityConfig) {
        this.publicPaths = securityConfig.getPublicPaths();

        if (securityConfig.getJwt().isEnabled()) {
            providers.add(new JwtAuthProvider(securityConfig.getJwt()));
        }
        if (securityConfig.getApiKey().isEnabled()) {
            providers.add(new ApiKeyAuthProvider(securityConfig.getApiKey()));
        }

        log.info("Authentication filter initialized with {} providers: {}",
                providers.size(), providers.stream().map(AuthenticationProvider::type).toList());
    }

    @Override
    public String name() {
        return "authentication";
    }

    @Override
    public int order() {
        return FilterOrder.AUTHENTICATION;
    }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        // Skip auth for public paths
        if (isPublicPath(context.getPath())) {
            log.debug("Public path access: {}", context.getPath());
            chain.next(context);
            return;
        }

        // Try each provider
        for (AuthenticationProvider provider : providers) {
            if (provider.supports(context)) {
                var result = provider.authenticate(context);
                if (result.isPresent()) {
                    AuthenticationResult auth = result.get();
                    context.setAuthenticatedSubject(auth.subject());
                    context.setRoles(auth.roles());
                    context.setAttribute("auth-type", auth.authType());

                    log.debug("Authenticated: subject={}, type={}, roles={}",
                            auth.subject(), auth.authType(), auth.roles());
                    chain.next(context);
                    return;
                }
            }
        }

        // No successful authentication
        log.warn("Authentication failed for request {} on path {}",
                context.getRequestId(), context.getPath());
        context.abort(401, "Authentication required. Provide valid JWT token or API key.");
    }

    private boolean isPublicPath(String path) {
        if (publicPaths == null) return false;
        return publicPaths.stream().anyMatch(pattern -> {
            if (pattern.endsWith("/**")) {
                return path.startsWith(pattern.substring(0, pattern.length() - 3));
            }
            return path.equals(pattern);
        });
    }

    /**
     * Get the JWT provider for token generation (admin use).
     */
    public JwtAuthProvider getJwtProvider() {
        return providers.stream()
                .filter(p -> p instanceof JwtAuthProvider)
                .map(p -> (JwtAuthProvider) p)
                .findFirst()
                .orElse(null);
    }
}

