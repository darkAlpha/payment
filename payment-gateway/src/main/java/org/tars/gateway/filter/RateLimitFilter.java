package org.tars.gateway.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "gateway.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final int maxRequestsPerMinute;
    private final Cache<String, AtomicInteger> counters;

    public RateLimitFilter(@Value("${gateway.rate-limit.requests-per-minute:100}") int rpm) {
        this.maxRequestsPerMinute = rpm;
        this.counters = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(1)).build();
    }

    @Override
    public int getOrder() { return -10; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String key = resolveKey(exchange);
        AtomicInteger counter = counters.get(key, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();

        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(maxRequestsPerMinute));
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining",
                String.valueOf(Math.max(0, maxRequestsPerMinute - current)));

        if (current > maxRequestsPerMinute) {
            log.warn("Rate limit exceeded: key={}", key);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("Retry-After", "60");
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    private String resolveKey(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null) return forwarded.split(",")[0].trim();
        var addr = exchange.getRequest().getRemoteAddress();
        return addr != null ? addr.getAddress().getHostAddress() : "unknown";
    }
}
