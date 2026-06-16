package org.tars.gateway.loadbalancer;

import org.tars.gateway.config.GatewayProperties;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LeastConnectionsStrategy implements LoadBalancerStrategy {
    private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();

    @Override public String getName() { return "least-connections"; }

    @Override
    public GatewayProperties.Upstream select(List<GatewayProperties.Upstream> upstreams, String key) {
        List<GatewayProperties.Upstream> healthy = upstreams.stream().filter(GatewayProperties.Upstream::isHealthy).toList();
        if (healthy.isEmpty()) throw new IllegalStateException("No healthy upstreams");
        var selected = healthy.stream()
                .min((a, b) -> Integer.compare(getCount(a.getUrl()), getCount(b.getUrl())))
                .orElse(healthy.getFirst());
        counts.computeIfAbsent(selected.getUrl(), k -> new AtomicInteger(0)).incrementAndGet();
        return selected;
    }

    public void release(String url) {
        counts.computeIfPresent(url, (k, v) -> { v.decrementAndGet(); return v; });
    }

    private int getCount(String url) {
        return counts.computeIfAbsent(url, k -> new AtomicInteger(0)).get();
    }
}
