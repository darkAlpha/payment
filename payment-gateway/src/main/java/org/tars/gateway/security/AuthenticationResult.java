package org.tars.gateway.security;

import java.util.Set;

/**
 * Result of a successful authentication.
 */
public record AuthenticationResult(
        String subject,
        Set<String> roles,
        String authType,
        long expiresAt
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasAnyRole(Set<String> requiredRoles) {
        if (requiredRoles == null || requiredRoles.isEmpty()) return true;
        return roles.stream().anyMatch(requiredRoles::contains);
    }
}

