package org.tars.gateway.loadbalancer;

import org.tars.gateway.config.GatewayProperties;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinStrategy implements LoadBalancerStrategy {
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override public String getName() { return "round-robin"; }

    @Override
    public GatewayProperties.Upstream select(List<GatewayProperties.Upstream> upstreams, String key) {
        List<GatewayProperties.Upstream> healthy = upstreams.stream().filter(GatewayProperties.Upstream::isHealthy).toList();
        if (healthy.isEmpty()) throw new IllegalStateException("No healthy upstreams");
        return healthy.get(Math.abs(counter.getAndIncrement() % healthy.size()));
    }
}
