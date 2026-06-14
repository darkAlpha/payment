package org.tars.gateway.security;

import org.tars.gateway.context.GatewayContext;

import java.util.Optional;

/**
 * Authentication provider interface.
 * Different implementations for JWT, API key, etc.
 */
public interface AuthenticationProvider {

    /**
     * Provider name/type identifier.
     */
    String type();

    /**
     * Check if this provider can handle the given request context.
     */
    boolean supports(GatewayContext context);

    /**
     * Authenticate the request. Returns authentication result.
     */
    Optional<AuthenticationResult> authenticate(GatewayContext context);
}

