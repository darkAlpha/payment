package org.tars.gateway.loadbalancer;

import org.tars.gateway.config.GatewayConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Weighted load balancing strategy.
 * Higher weight = more traffic to that upstream.
 */
public class WeightedStrategy implements LoadBalancerStrategy {

    @Override
    public String name() {
        return "weighted";
    }

    @Override
    public GatewayConfig.UpstreamConfig select(List<GatewayConfig.UpstreamConfig> upstreams, String key) {
        List<GatewayConfig.UpstreamConfig> healthy = upstreams.stream()
                .filter(GatewayConfig.UpstreamConfig::isHealthy)
                .toList();

        if (healthy.isEmpty()) {
            throw new IllegalStateException("No healthy upstreams available");
        }

        int totalWeight = healthy.stream().mapToInt(GatewayConfig.UpstreamConfig::getWeight).sum();
        int random = ThreadLocalRandom.current().nextInt(totalWeight);

        int cumulative = 0;
        for (GatewayConfig.UpstreamConfig upstream : healthy) {
            cumulative += upstream.getWeight();
            if (random < cumulative) {
                return upstream;
            }
        }

        return healthy.getLast();
    }
}

