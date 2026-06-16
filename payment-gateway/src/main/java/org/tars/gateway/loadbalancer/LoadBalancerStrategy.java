package org.tars.gateway.loadbalancer;

import org.tars.gateway.config.GatewayProperties;
import java.util.List;

public interface LoadBalancerStrategy {
    String getName();
    GatewayProperties.Upstream select(List<GatewayProperties.Upstream> upstreams, String key);
}
