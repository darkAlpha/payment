package org.tars.gateway.route;

import lombok.extern.slf4j.Slf4j;
import org.tars.gateway.config.GatewayProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
public class RouteRegistry {
    private final List<RouteEntry> entries = new ArrayList<>();

    public RouteRegistry(List<GatewayProperties.Route> routes) {
        for (var r : routes) {
            entries.add(new RouteEntry(r, compilePattern(r.getPath())));
        }
        log.info("Route registry: {} routes loaded", entries.size());
    }

    public Optional<GatewayProperties.Route> match(String method, String path) {
        return entries.stream()
                .filter(e -> matchMethod(e.route().getMethod(), method))
                .filter(e -> e.pattern().matcher(path).matches())
                .map(RouteEntry::route)
                .findFirst();
    }

    public List<GatewayProperties.Route> getAll() {
        return entries.stream().map(RouteEntry::route).toList();
    }

    private boolean matchMethod(String routeMethod, String requestMethod) {
        return "*".equals(routeMethod) || routeMethod.equalsIgnoreCase(requestMethod);
    }

    private Pattern compilePattern(String path) {
        String regex = path
                .replace("/**", "/.*")
                .replace("/*", "/[^/]+")
                .replaceAll("\\{[^}]+}", "[^/]+");
        if (!regex.endsWith(".*")) regex += "/?";
        return Pattern.compile("^" + regex + "$");
    }

    private record RouteEntry(GatewayProperties.Route route, Pattern pattern) {}
}
