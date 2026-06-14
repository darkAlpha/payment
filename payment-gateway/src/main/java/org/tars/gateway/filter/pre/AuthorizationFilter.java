package org.tars.gateway.filter.pre;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;
import org.tars.gateway.security.rbac.RbacManager;

import java.util.List;

/**
 * Authorization filter - enforces RBAC rules.
 * Checks if authenticated user has required roles/permissions for the route.
 */
public class AuthorizationFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationFilter.class);

    private final RbacManager rbacManager;

    public AuthorizationFilter(RbacManager rbacManager) {
        this.rbacManager = rbacManager;
    }

    @Override
    public String name() {
        return "authorization";
    }

    @Override
    public int order() {
        return FilterOrder.AUTHORIZATION;
    }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        // If no authentication, skip (auth filter already handled it)
        if (context.getAuthenticatedSubject() == null) {
            chain.next(context);
            return;
        }

        // Check route-specific required roles
        GatewayConfig.RouteConfig route = context.getAttribute("matched-route");
        if (route != null && route.getRequiredRoles() != null && !route.getRequiredRoles().isEmpty()) {
            if (!rbacManager.hasRequiredRoles(context.getRoles(), route.getRequiredRoles())) {
                log.warn("Authorization denied: subject={}, roles={}, required={}",
                        context.getAuthenticatedSubject(), context.getRoles(), route.getRequiredRoles());
                context.abort(403, "Insufficient permissions. Required roles: " + route.getRequiredRoles());
                return;
            }
        }

        // Check path-level RBAC
        if (!rbacManager.canAccess(context.getRoles(), context.getPath())) {
            log.warn("RBAC denied: subject={}, roles={}, path={}",
                    context.getAuthenticatedSubject(), context.getRoles(), context.getPath());
            context.abort(403, "Access denied for path: " + context.getPath());
            return;
        }

        chain.next(context);
    }
}

