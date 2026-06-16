package org.tars.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.tars.gateway.feature.FeatureFlagService;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "gateway.feature-flags", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FeatureFlagFilter implements GlobalFilter, Ordered {

    private final FeatureFlagService featureFlagService;

    public FeatureFlagFilter(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @Override
    public int getOrder() { return -5; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String feature = extractFeature(path);

        if (feature != null && !featureFlagService.isEnabled(feature)) {
            log.info("Feature disabled: {} for path {}", feature, path);
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    private String extractFeature(String path) {
        String[] parts = path.split("/");
        if (parts.length >= 4 && "api".equals(parts[1])) return parts[3];
        if (parts.length >= 3) return parts[2];
        return null;
    }
}
