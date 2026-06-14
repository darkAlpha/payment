package org.tars.gateway.versioning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.context.GatewayContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Version router - resolves which version of the upstream to use.
 * Supports header-based and percentage-based routing strategies.
 */
public class VersionRouter {

    private static final Logger log = LoggerFactory.getLogger(VersionRouter.class);

    private final Map<String, VersionStrategy> strategies = new ConcurrentHashMap<>();

    public VersionRouter() {
        strategies.put("header", new HeaderVersionStrategy());
        strategies.put("percentage", new PercentageVersionStrategy());
    }

    /**
     * Filter upstreams based on version resolution.
     */
    public List<GatewayConfig.UpstreamConfig> resolveUpstreams(
            GatewayContext context,
            GatewayConfig.RouteConfig route,
            List<GatewayConfig.UpstreamConfig> upstreams) {

        GatewayConfig.VersioningConfig versioningConfig = route.getVersioning();
        if (versioningConfig == null) {
            return upstreams; // No versioning configured
        }

        VersionStrategy strategy = strategies.get(versioningConfig.getStrategy());
        if (strategy == null) {
            log.warn("Unknown versioning strategy: {}. Skipping version routing.", versioningConfig.getStrategy());
            return upstreams;
        }

        String resolvedVersion = strategy.resolveVersion(context, versioningConfig);
        if (resolvedVersion == null || resolvedVersion.isBlank()) {
            return upstreams; // No version preference
        }

        log.debug("Request {} routed to version: {}", context.getRequestId(), resolvedVersion);
        context.setAttribute("resolved-version", resolvedVersion);

        // Filter upstreams by resolved version
        List<GatewayConfig.UpstreamConfig> versionedUpstreams = upstreams.stream()
                .filter(u -> resolvedVersion.equals(u.getVersion()))
                .toList();

        if (versionedUpstreams.isEmpty()) {
            log.warn("No upstreams found for version '{}', falling back to all upstreams", resolvedVersion);
            return upstreams;
        }

        return versionedUpstreams;
    }

    public void registerStrategy(String name, VersionStrategy strategy) {
        strategies.put(name, strategy);
    }
}

