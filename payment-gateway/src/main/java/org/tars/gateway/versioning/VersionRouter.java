package org.tars.gateway.versioning;

import lombok.extern.slf4j.Slf4j;
import org.tars.gateway.config.GatewayProperties;
import org.tars.gateway.context.GatewayContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class VersionRouter {
    private final Map<String, VersionStrategy> strategies = new ConcurrentHashMap<>();

    public VersionRouter() {
        strategies.put("header", new HeaderVersionStrategy());
        strategies.put("percentage", new PercentageVersionStrategy());
    }

    public List<GatewayProperties.Upstream> resolve(GatewayContext context, GatewayProperties.Route route) {
        GatewayProperties.Versioning vc = route.getVersioning();
        if (vc == null) return route.getUpstreams();

        VersionStrategy strategy = strategies.get(vc.getStrategy());
        if (strategy == null) return route.getUpstreams();

        String version = strategy.resolve(context, vc);
        if (version == null || version.isBlank()) return route.getUpstreams();

        context.setAttribute("resolved-version", version);
        List<GatewayProperties.Upstream> filtered = route.getUpstreams().stream()
                .filter(u -> version.equals(u.getVersion())).toList();
        if (filtered.isEmpty()) {
            log.warn("No upstreams for version '{}', using all", version);
            return route.getUpstreams();
        }
        return filtered;
    }
}
