package org.tars.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tars.gateway.loadbalancer.*;

/**
 * Registers all load balancer strategy beans.
 * Add new strategies here — they will be auto-discovered by LoadBalancerRegistry.
 */
@Configuration(proxyBeanMethods = false)
public class LoadBalancerConfiguration {

    @Bean
    public RoundRobinStrategy roundRobinStrategy() {
        return new RoundRobinStrategy();
    }

    @Bean
    public WeightedStrategy weightedStrategy() {
        return new WeightedStrategy();
    }

    @Bean
    public RandomStrategy randomStrategy() {
        return new RandomStrategy();
    }

    @Bean
    public LeastConnectionsStrategy leastConnectionsStrategy() {
        return new LeastConnectionsStrategy();
    }

    @Bean
    public IpHashStrategy ipHashStrategy() {
        return new IpHashStrategy();
    }
}

