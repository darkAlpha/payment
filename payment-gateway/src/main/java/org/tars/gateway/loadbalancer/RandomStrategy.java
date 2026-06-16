package org.tars.gateway.loadbalancer;

import org.tars.gateway.config.GatewayProperties;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomStrategy implements LoadBalancerStrategy {
    @Override public String getName() { return "random"; }

    @Override
    public GatewayProperties.Upstream select(List<GatewayProperties.Upstream> upstreams, String key) {
        List<GatewayProperties.Upstream> healthy = upstreams.stream().filter(GatewayProperties.Upstream::isHealthy).toList();
        if (healthy.isEmpty()) throw new IllegalStateException("No healthy upstreams");
        return healthy.get(ThreadLocalRandom.current().nextInt(healthy.size()));
    }
}
