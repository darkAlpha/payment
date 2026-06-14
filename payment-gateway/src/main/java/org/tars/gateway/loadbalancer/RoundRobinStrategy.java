package org.tars.gateway.loadbalancer;

import org.tars.gateway.config.GatewayConfig;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round-robin load balancing strategy.
 * Distributes requests evenly across all healthy upstreams.
 */
public class RoundRobinStrategy implements LoadBalancerStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public String name() {
        return "round-robin";
    }

    @Override
    public GatewayConfig.UpstreamConfig select(List<GatewayConfig.UpstreamConfig> upstreams, String key) {
        List<GatewayConfig.UpstreamConfig> healthy = upstreams.stream()
                .filter(GatewayConfig.UpstreamConfig::isHealthy)
                .toList();

        if (healthy.isEmpty()) {
            throw new IllegalStateException("No healthy upstreams available");
        }

        int index = Math.abs(counter.getAndIncrement() % healthy.size());
        return healthy.get(index);
    }
}

