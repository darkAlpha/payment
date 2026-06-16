package org.tars.gateway.loadbalancer;

import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class LoadBalancerRegistry {
    private final Map<String, LoadBalancerStrategy> strategies;

    public LoadBalancerRegistry(List<LoadBalancerStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(LoadBalancerStrategy::getName, Function.identity()));
        log.info("Load balancer strategies registered: {}", strategies.keySet());
    }

    public LoadBalancerStrategy get(String name) {
        LoadBalancerStrategy s = strategies.get(name);
        if (s == null) throw new IllegalArgumentException("Unknown LB strategy: " + name + ". Available: " + strategies.keySet());
        return s;
    }

    public Map<String, LoadBalancerStrategy> getAll() { return Map.copyOf(strategies); }
}
