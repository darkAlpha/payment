package org.tars.gateway.loadbalancer;

import org.tars.gateway.config.GatewayConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Least-connections load balancing strategy.
 * Routes to the upstream with fewest active connections.
 */
public class LeastConnectionsStrategy implements LoadBalancerStrategy {

    private final Map<String, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "least-connections";
    }

    @Override
    public GatewayConfig.UpstreamConfig select(List<GatewayConfig.UpstreamConfig> upstreams, String key) {
        List<GatewayConfig.UpstreamConfig> healthy = upstreams.stream()
                .filter(GatewayConfig.UpstreamConfig::isHealthy)
                .toList();

        if (healthy.isEmpty()) {
            throw new IllegalStateException("No healthy upstreams available");
        }

        GatewayConfig.UpstreamConfig selected = healthy.stream()
                .min((a, b) -> {
                    int countA = getConnectionCount(a.getUrl());
                    int countB = getConnectionCount(b.getUrl());
                    return Integer.compare(countA, countB);
                })
                .orElse(healthy.getFirst());

        incrementConnection(selected.getUrl());
        return selected;
    }

    public void releaseConnection(String upstreamUrl) {
        connectionCounts.computeIfPresent(upstreamUrl, (k, v) -> {
            v.decrementAndGet();
            return v;
        });
    }

    private int getConnectionCount(String url) {
        return connectionCounts.computeIfAbsent(url, k -> new AtomicInteger(0)).get();
    }

    private void incrementConnection(String url) {
        connectionCounts.computeIfAbsent(url, k -> new AtomicInteger(0)).incrementAndGet();
    }
}

