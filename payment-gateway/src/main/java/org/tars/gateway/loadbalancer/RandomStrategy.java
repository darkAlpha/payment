package org.tars.gateway.loadbalancer;

import org.tars.gateway.config.GatewayConfig;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Random load balancing strategy.
 * Selects a random healthy upstream for each request.
 */
public class RandomStrategy implements LoadBalancerStrategy {

    @Override
    public String name() {
        return "random";
    }

    @Override
    public GatewayConfig.UpstreamConfig select(List<GatewayConfig.UpstreamConfig> upstreams, String key) {
        List<GatewayConfig.UpstreamConfig> healthy = upstreams.stream()
                .filter(GatewayConfig.UpstreamConfig::isHealthy)
                .toList();

        if (healthy.isEmpty()) {
            throw new IllegalStateException("No healthy upstreams available");
        }

        int index = ThreadLocalRandom.current().nextInt(healthy.size());
        return healthy.get(index);
    }
}

