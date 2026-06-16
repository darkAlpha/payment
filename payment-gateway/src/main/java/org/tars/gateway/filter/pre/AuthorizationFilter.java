package org.tars.gateway.filter.pre;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tars.gateway.config.GatewayProperties;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;
import org.tars.gateway.security.rbac.RbacService;

/**
 * Authorization filter — enforces RBAC rules.
 */
@Slf4j
@Component
public class AuthorizationFilter implements GatewayFilter {

    private final RbacService rbacService;

    public AuthorizationFilter(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @Override public String getName() { return "authorization"; }
    @Override public int getOrder() { return FilterOrder.AUTHORIZATION; }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        if (context.getAuthenticatedSubject() == null) {
            chain.next(context);
            return;
        }

        GatewayProperties.Route route = context.getAttribute("matched-route");
        if (route != null && !route.getRequiredRoles().isEmpty()) {
            boolean hasRole = route.getRequiredRoles().stream().anyMatch(context.getRoles()::contains);
            if (!hasRole) {
                log.warn("[{}] Forbidden: subject={}, roles={}, required={}",
                        context.getRequestId(), context.getAuthenticatedSubject(),
                        context.getRoles(), route.getRequiredRoles());
                context.abort(403, "Insufficient permissions");
                return;
            }
        }

        if (!rbacService.canAccess(context.getRoles(), context.getPath())) {
            context.abort(403, "Access denied");
            return;
        }

        chain.next(context);
    }
}

