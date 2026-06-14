package org.tars.gateway.metrics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Simple metrics collector for gateway monitoring.
 * Thread-safe counters for request tracking.
 */
public class MetricsCollector {

    private static final MetricsCollector INSTANCE = new MetricsCollector();

    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder totalErrors = new LongAdder();
    private final AtomicLong totalLatencyMs = new AtomicLong(0);
    private final Map<String, LongAdder> statusCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> pathCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> methodCounts = new ConcurrentHashMap<>();

    private MetricsCollector() {}

    public static MetricsCollector getInstance() {
        return INSTANCE;
    }

    public void recordRequest(String path, String method, int status, long latencyMs) {
        totalRequests.increment();
        totalLatencyMs.addAndGet(latencyMs);

        if (status >= 400) {
            totalErrors.increment();
        }

        statusCounts.computeIfAbsent(String.valueOf(status), k -> new LongAdder()).increment();
        pathCounts.computeIfAbsent(normalizePath(path), k -> new LongAdder()).increment();
        methodCounts.computeIfAbsent(method, k -> new LongAdder()).increment();
    }

    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        long total = totalRequests.sum();

        metrics.put("totalRequests", total);
        metrics.put("totalErrors", totalErrors.sum());
        metrics.put("avgLatencyMs", total > 0 ? totalLatencyMs.get() / total : 0);
        metrics.put("errorRate", total > 0 ? String.format("%.2f%%", (totalErrors.sum() * 100.0) / total) : "0%");

        Map<String, Long> statusMap = new LinkedHashMap<>();
        statusCounts.forEach((k, v) -> statusMap.put(k, v.sum()));
        metrics.put("byStatus", statusMap);

        Map<String, Long> methodMap = new LinkedHashMap<>();
        methodCounts.forEach((k, v) -> methodMap.put(k, v.sum()));
        metrics.put("byMethod", methodMap);

        Map<String, Long> pathMap = new LinkedHashMap<>();
        pathCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().sum(), a.getValue().sum()))
                .limit(20)
                .forEach(e -> pathMap.put(e.getKey(), e.getValue().sum()));
        metrics.put("topPaths", pathMap);

        return metrics;
    }

    /**
     * Normalize path to group similar paths (replace UUIDs/numbers with placeholders).
     */
    private String normalizePath(String path) {
        return path.replaceAll("/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "/{id}")
                   .replaceAll("/\\d+", "/{id}");
    }

    public void reset() {
        totalRequests.reset();
        totalErrors.reset();
        totalLatencyMs.set(0);
        statusCounts.clear();
        pathCounts.clear();
        methodCounts.clear();
    }
}

