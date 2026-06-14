package org.tars.gateway.security.rbac;

import java.util.Set;

/**
 * RBAC Role definition with associated permissions.
 */
public record Role(
        String name,
        Set<Permission> permissions,
        Set<String> allowedPaths
) {
    public boolean hasPermission(Permission permission) {
        return permissions.stream().anyMatch(p ->
                (p.resource().equals("*") || p.resource().equals(permission.resource())) &&
                (p.action().equals("*") || p.action().equals(permission.action()))
        );
    }

    public boolean canAccessPath(String path) {
        if (allowedPaths == null || allowedPaths.isEmpty()) return true;
        return allowedPaths.stream().anyMatch(pattern -> {
            if (pattern.equals("*") || pattern.equals("/**")) return true;
            if (pattern.endsWith("/**")) {
                return path.startsWith(pattern.substring(0, pattern.length() - 3));
            }
            return path.equals(pattern);
        });
    }
}

