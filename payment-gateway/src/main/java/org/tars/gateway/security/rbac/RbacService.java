package org.tars.gateway.security.rbac;

import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RbacService {
    private final Map<String, RoleDefinition> roles = new ConcurrentHashMap<>();

    public RbacService() { initDefaults(); }

    private void initDefaults() {
        register("ADMIN", Set.of("/**"));
        register("OPERATOR", Set.of("/api/v1/deposits/**", "/api/v1/transfers/**", "/api/v1/accounts/**"));
        register("VIEWER", Set.of("/api/v1/**"));
        register("SERVICE", Set.of("/api/**", "/internal/**"));
    }

    public void register(String role, Set<String> paths) {
        roles.put(role, new RoleDefinition(role, paths));
    }

    public boolean canAccess(Set<String> userRoles, String path) {
        if (userRoles == null || userRoles.isEmpty()) return false;
        return userRoles.stream().map(roles::get).filter(Objects::nonNull).anyMatch(r -> r.canAccessPath(path));
    }

    public record RoleDefinition(String name, Set<String> allowedPaths) {
        public boolean canAccessPath(String path) {
            return allowedPaths.stream().anyMatch(pattern -> {
                if ("/**".equals(pattern) || "*".equals(pattern)) return true;
                if (pattern.endsWith("/**")) return path.startsWith(pattern.substring(0, pattern.length() - 3));
                return path.equals(pattern);
            });
        }
    }
}
