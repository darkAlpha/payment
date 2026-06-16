package org.tars.gateway.loadbalancer;

import org.tars.gateway.config.GatewayProperties;
import java.util.List;

public class IpHashStrategy implements LoadBalancerStrategy {
    @Override public String getName() { return "ip-hash"; }

    @Override
    public GatewayProperties.Upstream select(List<GatewayProperties.Upstream> upstreams, String key) {
        List<GatewayProperties.Upstream> healthy = upstreams.stream().filter(GatewayProperties.Upstream::isHealthy).toList();
        if (healthy.isEmpty()) throw new IllegalStateException("No healthy upstreams");
        if (key == null || key.isBlank()) return healthy.getFirst();
        return healthy.get(Math.abs(key.hashCode()) % healthy.size());
    }
}
