package org.tars.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tars.gateway.feature.FeatureFlagService;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.loadbalancer.*;
import org.tars.gateway.proxy.NettyProxyClient;
import org.tars.gateway.route.RouteRegistry;
import org.tars.gateway.security.rbac.RbacService;
import org.tars.gateway.versioning.VersionRouter;

import java.util.Comparator;
import java.util.List;

/**
 * Central gateway bean configuration.
 * Defines infrastructure beans and assembles the filter pipeline.
 */
@Configuration(proxyBeanMethods = false)
public class GatewayConfiguration {

    @Bean
    public ObjectMapper gatewayObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean
    public RouteRegistry routeRegistry(GatewayProperties properties) {
        return new RouteRegistry(properties.getRoutes());
    }

    @Bean
    public RbacService rbacService() {
        return new RbacService();
    }

    @Bean
    public FeatureFlagService featureFlagService(GatewayProperties properties) {
        return new FeatureFlagService(properties.getFeatureFlags());
    }

    @Bean
    public VersionRouter versionRouter() {
        return new VersionRouter();
    }

    @Bean
    public NettyProxyClient nettyProxyClient() {
        return new NettyProxyClient();
    }

    @Bean
    public LoadBalancerRegistry loadBalancerRegistry(List<LoadBalancerStrategy> strategies) {
        return new LoadBalancerRegistry(strategies);
    }

    /**
     * Collects all GatewayFilter beans and returns them sorted by order.
     */
    @Bean
    public List<GatewayFilter> orderedFilters(List<GatewayFilter> filters) {
        return filters.stream()
                .sorted(Comparator.comparingInt(GatewayFilter::getOrder))
                .toList();
    }
}

