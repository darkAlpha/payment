package org.tars.gateway.security.rbac;

import java.util.Set;

/**
 * RBAC Permission model.
 */
public record Permission(
        String resource,
        String action
) {
    public static Permission of(String resource, String action) {
        return new Permission(resource, action);
    }

    public static Permission parse(String permission) {
        String[] parts = permission.split(":");
        if (parts.length == 2) {
            return new Permission(parts[0], parts[1]);
        }
        return new Permission(permission, "*");
    }
}

