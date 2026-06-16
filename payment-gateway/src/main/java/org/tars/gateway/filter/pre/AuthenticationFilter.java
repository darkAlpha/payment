package org.tars.gateway.filter.pre;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tars.gateway.config.GatewayProperties;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;
import org.tars.gateway.security.AuthenticationResult;
import org.tars.gateway.security.jwt.JwtAuthProvider;
import org.tars.gateway.security.apikey.ApiKeyAuthProvider;

import java.util.List;
import java.util.Optional;

/**
 * Authenticates requests using JWT or API key.
 * Skips public paths.
 */
@Slf4j
@Component
public class AuthenticationFilter implements GatewayFilter {

    private final JwtAuthProvider jwtProvider;
    private final ApiKeyAuthProvider apiKeyProvider;
    private final List<String> publicPaths;

    public AuthenticationFilter(GatewayProperties properties,
                                JwtAuthProvider jwtProvider,
                                ApiKeyAuthProvider apiKeyProvider) {
        this.jwtProvider = jwtProvider;
        this.apiKeyProvider = apiKeyProvider;
        this.publicPaths = properties.getSecurity().getPublicPaths();
    }

    @Override public String getName() { return "authentication"; }
    @Override public int getOrder() { return FilterOrder.AUTHENTICATION; }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        if (isPublicPath(context.getPath())) {
            chain.next(context);
            return;
        }

        // Try JWT first, then API key
        Optional<AuthenticationResult> result = jwtProvider.authenticate(context)
                .or(() -> apiKeyProvider.authenticate(context));

        if (result.isPresent()) {
            AuthenticationResult auth = result.get();
            context.setAuthenticatedSubject(auth.subject());
            context.setRoles(auth.roles());
            context.setAttribute("auth-type", auth.authType());
            log.debug("[{}] Authenticated: subject={}, type={}", context.getRequestId(), auth.subject(), auth.authType());
            chain.next(context);
        } else {
            log.warn("[{}] Authentication failed: path={}", context.getRequestId(), context.getPath());
            context.abort(401, "Authentication required");
        }
    }

    private boolean isPublicPath(String path) {
        return publicPaths != null && publicPaths.stream().anyMatch(p -> {
            if (p.endsWith("/**")) return path.startsWith(p.substring(0, p.length() - 3));
            return path.equals(p);
        });
    }
}

