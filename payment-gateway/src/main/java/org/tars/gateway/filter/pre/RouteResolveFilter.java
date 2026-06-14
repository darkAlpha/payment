package org.tars.gateway.filter.pre;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;
import org.tars.gateway.loadbalancer.LoadBalancerFactory;
import org.tars.gateway.loadbalancer.LoadBalancerStrategy;
import org.tars.gateway.versioning.VersionRouter;

import java.util.List;

/**
 * Route resolution and load balancing filter.
 * Resolves the target upstream based on version routing and load balancing strategy.
 */
public class RouteResolveFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(RouteResolveFilter.class);

    private final VersionRouter versionRouter;

    public RouteResolveFilter(VersionRouter versionRouter) {
        this.versionRouter = versionRouter;
    }

    @Override
    public String name() {
        return "route-resolve";
    }

    @Override
    public int order() {
        return FilterOrder.LOAD_BALANCE;
    }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        GatewayConfig.RouteConfig route = context.getAttribute("matched-route");
        if (route == null) {
            context.abort(404, "No route matched for: " + context.getMethod() + " " + context.getPath());
            return;
        }

        List<GatewayConfig.UpstreamConfig> upstreams = route.getUpstreams();
        if (upstreams == null || upstreams.isEmpty()) {
            context.abort(503, "No upstreams configured for route: " + route.getId());
            return;
        }

        // Apply version routing
        List<GatewayConfig.UpstreamConfig> versionedUpstreams = versionRouter.resolveUpstreams(context, route, upstreams);

        // Apply load balancing
        try {
            LoadBalancerStrategy strategy = LoadBalancerFactory.get(route.getLoadBalancer());
            String clientIp = context.getHeader("X-Forwarded-For");
            GatewayConfig.UpstreamConfig selected = strategy.select(versionedUpstreams, clientIp);

            String targetUrl = buildTargetUrl(selected.getUrl(), context.getPath(), route);
            context.setResolvedUpstream(targetUrl);

            log.debug("Request {} routed to: {} (strategy={}, version={})",
                    context.getRequestId(), targetUrl, strategy.name(), selected.getVersion());

        } catch (IllegalStateException e) {
            log.error("Load balancing failed for route {}: {}", route.getId(), e.getMessage());
            context.abort(503, "No healthy upstream available");
            return;
        }

        chain.next(context);
    }

    private String buildTargetUrl(String upstreamUrl, String path, GatewayConfig.RouteConfig route) {
        String targetPath = path;
        if (route.isStripPrefix() && route.getPath() != null) {
            String prefix = route.getPath().replace("/**", "").replace("/*", "");
            if (path.startsWith(prefix)) {
                targetPath = path.substring(prefix.length());
                if (!targetPath.startsWith("/")) targetPath = "/" + targetPath;
            }
        }

        String baseUrl = upstreamUrl.endsWith("/") ? upstreamUrl.substring(0, upstreamUrl.length() - 1) : upstreamUrl;
        return baseUrl + targetPath;
    }
}

