package org.tars.gateway.loadbalancer;

import org.tars.gateway.config.GatewayProperties;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WeightedStrategy implements LoadBalancerStrategy {
    @Override public String getName() { return "weighted"; }

    @Override
    public GatewayProperties.Upstream select(List<GatewayProperties.Upstream> upstreams, String key) {
        List<GatewayProperties.Upstream> healthy = upstreams.stream().filter(GatewayProperties.Upstream::isHealthy).toList();
        if (healthy.isEmpty()) throw new IllegalStateException("No healthy upstreams");
        int total = healthy.stream().mapToInt(GatewayProperties.Upstream::getWeight).sum();
        int rand = ThreadLocalRandom.current().nextInt(total);
        int cum = 0;
        for (var u : healthy) { cum += u.getWeight(); if (rand < cum) return u; }
        return healthy.getLast();
    }
}
