package org.tars.gateway.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.config.GatewayConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Route registry - matches incoming requests to configured routes.
 * Supports path patterns with wildcards.
 */
public class RouteRegistry {

    private static final Logger log = LoggerFactory.getLogger(RouteRegistry.class);

    private final List<RouteEntry> routes = new ArrayList<>();
    private final Map<String, GatewayConfig.RouteConfig> routeById = new ConcurrentHashMap<>();

    public RouteRegistry(List<GatewayConfig.RouteConfig> routeConfigs) {
        for (GatewayConfig.RouteConfig config : routeConfigs) {
            Pattern pattern = compilePathPattern(config.getPath());
            routes.add(new RouteEntry(config, pattern));
            routeById.put(config.getId(), config);
        }
        log.info("Route registry initialized with {} routes", routes.size());
    }

    /**
     * Find matching route for the given method and path.
     */
    public Optional<GatewayConfig.RouteConfig> match(String method, String path) {
        return routes.stream()
                .filter(entry -> matchesMethod(entry.config().getMethod(), method))
                .filter(entry -> entry.pattern().matcher(path).matches())
                .map(RouteEntry::config)
                .findFirst();
    }

    public Optional<GatewayConfig.RouteConfig> getById(String routeId) {
        return Optional.ofNullable(routeById.get(routeId));
    }

    public List<GatewayConfig.RouteConfig> getAllRoutes() {
        return routes.stream().map(RouteEntry::config).toList();
    }

    private boolean matchesMethod(String routeMethod, String requestMethod) {
        if ("*".equals(routeMethod)) return true;
        return routeMethod.equalsIgnoreCase(requestMethod);
    }

    /**
     * Convert path pattern to regex.
     * Supports: /api/** (prefix match), /api/{id} (path variable), /api/* (single segment)
     */
    private Pattern compilePathPattern(String path) {
        String regex = path
                .replace("/**", "/.*")
                .replace("/*", "/[^/]+")
                .replaceAll("\\{[^}]+}", "[^/]+");

        // Ensure exact match unless wildcard
        if (!regex.endsWith(".*")) {
            regex = regex + "/?";
        }

        return Pattern.compile("^" + regex + "$");
    }

    private record RouteEntry(GatewayConfig.RouteConfig config, Pattern pattern) {}
}

