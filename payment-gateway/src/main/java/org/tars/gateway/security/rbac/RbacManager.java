package org.tars.gateway.security.rbac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RBAC Manager - manages roles and permissions.
 * Supports dynamic role/permission configuration.
 */
public class RbacManager {

    private static final Logger log = LoggerFactory.getLogger(RbacManager.class);

    private final Map<String, Role> roles = new ConcurrentHashMap<>();

    public RbacManager() {
        initializeDefaultRoles();
    }

    private void initializeDefaultRoles() {
        // Admin - full access
        roles.put("ADMIN", new Role("ADMIN",
                Set.of(Permission.of("*", "*")),
                Set.of("/**")));

        // Operator - read/write on operations
        roles.put("OPERATOR", new Role("OPERATOR",
                Set.of(
                        Permission.of("deposit", "create"),
                        Permission.of("deposit", "read"),
                        Permission.of("deposit", "close"),
                        Permission.of("transfer", "create"),
                        Permission.of("transfer", "read")
                ),
                Set.of("/api/v1/deposits/**", "/api/v1/transfers/**")));

        // Viewer - read only
        roles.put("VIEWER", new Role("VIEWER",
                Set.of(
                        Permission.of("deposit", "read"),
                        Permission.of("transfer", "read"),
                        Permission.of("account", "read")
                ),
                Set.of("/api/v1/**")));

        // Service-to-service
        roles.put("SERVICE", new Role("SERVICE",
                Set.of(Permission.of("*", "*")),
                Set.of("/internal/**", "/api/**")));

        log.info("RBAC initialized with {} roles", roles.size());
    }

    public void addRole(Role role) {
        roles.put(role.name(), role);
    }

    public Optional<Role> getRole(String roleName) {
        return Optional.ofNullable(roles.get(roleName));
    }

    /**
     * Check if any of the user's roles can access the given path.
     */
    public boolean canAccess(Set<String> userRoles, String path) {
        if (userRoles == null || userRoles.isEmpty()) return false;
        return userRoles.stream()
                .map(roles::get)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.canAccessPath(path));
    }

    /**
     * Check if user has required roles for a route.
     */
    public boolean hasRequiredRoles(Set<String> userRoles, List<String> requiredRoles) {
        if (requiredRoles == null || requiredRoles.isEmpty()) return true;
        if (userRoles == null || userRoles.isEmpty()) return false;
        return requiredRoles.stream().anyMatch(userRoles::contains);
    }

    /**
     * Check specific permission.
     */
    public boolean hasPermission(Set<String> userRoles, String resource, String action) {
        Permission required = Permission.of(resource, action);
        return userRoles.stream()
                .map(roles::get)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.hasPermission(required));
    }
}

