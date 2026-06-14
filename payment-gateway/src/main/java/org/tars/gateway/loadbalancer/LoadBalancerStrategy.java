package org.tars.gateway.loadbalancer;

import org.tars.gateway.config.GatewayConfig;

import java.util.List;

/**
 * Load balancer strategy interface.
 * Selects an upstream server from the available pool.
 */
public interface LoadBalancerStrategy {

    /**
     * Strategy name identifier.
     */
    String name();

    /**
     * Select an upstream from the provided list.
     *
     * @param upstreams available upstream configurations
     * @param key optional sticky key (e.g., client IP, session ID)
     * @return selected upstream URL
     */
    GatewayConfig.UpstreamConfig select(List<GatewayConfig.UpstreamConfig> upstreams, String key);
}

