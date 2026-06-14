package org.tars.gateway.health;

import org.tars.gateway.feature.FeatureFlagManager;
import org.tars.gateway.route.RouteRegistry;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check handler - provides gateway status information.
 */
public class HealthCheckHandler {

    private final RouteRegistry routeRegistry;
    private final FeatureFlagManager featureFlagManager;
    private final Instant startTime;

    public HealthCheckHandler(RouteRegistry routeRegistry, FeatureFlagManager featureFlagManager) {
        this.routeRegistry = routeRegistry;
        this.featureFlagManager = featureFlagManager;
        this.startTime = Instant.now();
    }

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("uptime", getUptime());
        health.put("routes", routeRegistry.getAllRoutes().size());
        health.put("features", featureFlagManager.getAllFlags().size());

        // JVM info
        Map<String, Object> jvm = new LinkedHashMap<>();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        jvm.put("heapUsed", memory.getHeapMemoryUsage().getUsed() / (1024 * 1024) + "MB");
        jvm.put("heapMax", memory.getHeapMemoryUsage().getMax() / (1024 * 1024) + "MB");
        jvm.put("processors", Runtime.getRuntime().availableProcessors());
        jvm.put("javaVersion", runtime.getVmVersion());
        health.put("jvm", jvm);

        return health;
    }

    private String getUptime() {
        long seconds = java.time.Duration.between(startTime, Instant.now()).getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%dh %dm %ds", hours, minutes, secs);
    }
}

