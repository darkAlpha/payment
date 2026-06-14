package org.tars.gateway.loadbalancer;

import org.tars.gateway.config.GatewayConfig;

import java.util.List;

/**
 * IP Hash (sticky) load balancing strategy.
 * Same key always routes to same upstream (consistent hashing).
 */
public class IpHashStrategy implements LoadBalancerStrategy {

    @Override
    public String name() {
        return "ip-hash";
    }

    @Override
    public GatewayConfig.UpstreamConfig select(List<GatewayConfig.UpstreamConfig> upstreams, String key) {
        List<GatewayConfig.UpstreamConfig> healthy = upstreams.stream()
                .filter(GatewayConfig.UpstreamConfig::isHealthy)
                .toList();

        if (healthy.isEmpty()) {
            throw new IllegalStateException("No healthy upstreams available");
        }

        if (key == null || key.isBlank()) {
            return healthy.getFirst();
        }

        int hash = Math.abs(key.hashCode());
        int index = hash % healthy.size();
        return healthy.get(index);
    }
}

