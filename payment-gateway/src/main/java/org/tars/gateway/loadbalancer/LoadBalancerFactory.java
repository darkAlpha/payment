package org.tars.gateway.loadbalancer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for load balancer strategies.
 * Supports registration of custom strategies.
 */
public class LoadBalancerFactory {

    private static final Map<String, LoadBalancerStrategy> STRATEGIES = new ConcurrentHashMap<>();

    static {
        register(new RoundRobinStrategy());
        register(new WeightedStrategy());
        register(new RandomStrategy());
        register(new LeastConnectionsStrategy());
        register(new IpHashStrategy());
    }

    private LoadBalancerFactory() {}

    public static void register(LoadBalancerStrategy strategy) {
        STRATEGIES.put(strategy.name(), strategy);
    }

    public static LoadBalancerStrategy get(String name) {
        LoadBalancerStrategy strategy = STRATEGIES.get(name);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown load balancer strategy: " + name
                    + ". Available: " + STRATEGIES.keySet());
        }
        return strategy;
    }

    public static Map<String, LoadBalancerStrategy> all() {
        return Map.copyOf(STRATEGIES);
    }
}

