package org.tars.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Component
public class AccessLogFilter implements GlobalFilter, Ordered {

    private static final org.slf4j.Logger ACCESS = org.slf4j.LoggerFactory.getLogger("gateway.access");

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 12);
        exchange.getAttributes().put("requestId", requestId);
        exchange.getResponse().getHeaders().add("X-Request-Id", requestId);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long ms = System.currentTimeMillis() - start;
            var req = exchange.getRequest();
            var resp = exchange.getResponse();
            ACCESS.info("{} | {} {} | {} | {}ms",
                    requestId, req.getMethod(), req.getURI().getPath(),
                    resp.getStatusCode() != null ? resp.getStatusCode().value() : "-", ms);
        }));
    }
}
