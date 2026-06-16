package org.tars.gateway.health;

import org.springframework.stereotype.Service;
import org.tars.gateway.feature.FeatureFlagService;
import org.tars.gateway.route.RouteRegistry;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class HealthService {
    private final Instant startTime = Instant.now();
    private final RouteRegistry routeRegistry;
    private final FeatureFlagService featureFlagService;

    public HealthService(RouteRegistry routeRegistry, FeatureFlagService featureFlagService) {
        this.routeRegistry = routeRegistry;
        this.featureFlagService = featureFlagService;
    }

    public Map<String, Object> status() {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("status", "UP");
        h.put("uptime", formatDuration(Duration.between(startTime, Instant.now())));
        h.put("routes", routeRegistry.getAll().size());
        h.put("features", featureFlagService.getAll().size());
        var mem = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        h.put("heapUsedMB", mem.getUsed() / (1024 * 1024));
        h.put("heapMaxMB", mem.getMax() / (1024 * 1024));
        h.put("processors", Runtime.getRuntime().availableProcessors());
        return h;
    }

    private String formatDuration(Duration d) {
        return String.format("%dh %dm %ds", d.toHours(), d.toMinutesPart(), d.toSecondsPart());
    }
}
