package org.tars.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class VersionRoutingFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() { return -3; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String version = exchange.getRequest().getHeaders().getFirst("X-API-Version");
        if (version != null && !version.isBlank()) {
            exchange.getAttributes().put("api-version", version);
            log.debug("API version header: {}", version);
        }
        return chain.filter(exchange);
    }
}
