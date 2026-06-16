package org.tars.gateway.metrics;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Service
public class MetricsService {
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder totalErrors = new LongAdder();
    private final AtomicLong totalLatency = new AtomicLong(0);
    private final Map<String, LongAdder> statusCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> pathCounts = new ConcurrentHashMap<>();

    public void record(String path, String method, int status, long latencyMs) {
        totalRequests.increment();
        totalLatency.addAndGet(latencyMs);
        if (status >= 400) totalErrors.increment();
        statusCounts.computeIfAbsent(String.valueOf(status), k -> new LongAdder()).increment();
        pathCounts.computeIfAbsent(normalize(path), k -> new LongAdder()).increment();
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        long total = totalRequests.sum();
        m.put("totalRequests", total);
        m.put("totalErrors", totalErrors.sum());
        m.put("avgLatencyMs", total > 0 ? totalLatency.get() / total : 0);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        statusCounts.forEach((k, v) -> byStatus.put(k, v.sum()));
        m.put("byStatus", byStatus);
        Map<String, Long> topPaths = new LinkedHashMap<>();
        pathCounts.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue().sum(), a.getValue().sum()))
                .limit(20).forEach(e -> topPaths.put(e.getKey(), e.getValue().sum()));
        m.put("topPaths", topPaths);
        return m;
    }

    private String normalize(String path) {
        return path.replaceAll("/[0-9a-f-]{36}", "/{id}").replaceAll("/\\d+", "/{id}");
    }
}
