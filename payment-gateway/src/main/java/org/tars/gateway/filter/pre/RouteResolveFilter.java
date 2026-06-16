package org.tars.gateway.filter.pre;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tars.gateway.config.GatewayProperties;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;
import org.tars.gateway.loadbalancer.LoadBalancerRegistry;
import org.tars.gateway.loadbalancer.LoadBalancerStrategy;
import org.tars.gateway.route.RouteRegistry;
import org.tars.gateway.versioning.VersionRouter;

import java.util.List;
import java.util.Optional;

/**
 * Resolves route → applies versioning → load-balances to an upstream.
 */
@Slf4j
@Component
public class RouteResolveFilter implements GatewayFilter {

    private final RouteRegistry routeRegistry;
    private final VersionRouter versionRouter;
    private final LoadBalancerRegistry lbRegistry;

    public RouteResolveFilter(RouteRegistry routeRegistry, VersionRouter versionRouter,
                              LoadBalancerRegistry lbRegistry) {
        this.routeRegistry = routeRegistry;
        this.versionRouter = versionRouter;
        this.lbRegistry = lbRegistry;
    }

    @Override public String getName() { return "route-resolve"; }
    @Override public int getOrder() { return FilterOrder.LOAD_BALANCE; }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        Optional<GatewayProperties.Route> matched = routeRegistry.match(context.getMethod(), context.getPath());
        if (matched.isEmpty()) {
            context.abort(404, "No route for: " + context.getMethod() + " " + context.getPath());
            return;
        }

        GatewayProperties.Route route = matched.get();
        context.setAttribute("matched-route", route);

        List<GatewayProperties.Upstream> upstreams = route.getUpstreams();
        if (upstreams == null || upstreams.isEmpty()) {
            context.abort(503, "No upstreams for route: " + route.getId());
            return;
        }

        // Version routing
        List<GatewayProperties.Upstream> resolved = versionRouter.resolve(context, route);

        // Load balance
        LoadBalancerStrategy strategy = lbRegistry.get(route.getLoadBalancer());
        String clientKey = context.getHeader("X-Forwarded-For");
        GatewayProperties.Upstream selected = strategy.select(resolved, clientKey);

        String targetUrl = buildTargetUrl(selected.getUrl(), context.getPath(), route);
        context.setResolvedUpstream(targetUrl);

        log.debug("[{}] Routed to {} via {}", context.getRequestId(), targetUrl, strategy.getName());
        chain.next(context);
    }

    private String buildTargetUrl(String baseUrl, String path, GatewayProperties.Route route) {
        String targetPath = path;
        if (route.isStripPrefix()) {
            String prefix = route.getPath().replaceAll("/\\*\\*$", "").replaceAll("/\\*$", "");
            if (path.startsWith(prefix)) {
                targetPath = path.substring(prefix.length());
                if (!targetPath.startsWith("/")) targetPath = "/" + targetPath;
            }
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + targetPath;
    }
}

