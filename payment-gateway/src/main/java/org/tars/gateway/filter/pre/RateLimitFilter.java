package org.tars.gateway.filter.pre;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.tars.gateway.config.GatewayProperties;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter using Resilience4j.
 * Conditionally enabled via gateway.rate-limit.enabled=true.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "gateway.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter implements GatewayFilter {

    private final GatewayProperties.RateLimit config;
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    public RateLimitFilter(GatewayProperties properties) {
        this.config = properties.getRateLimit();
    }

    @Override public String getName() { return "rate-limit"; }
    @Override public int getOrder() { return FilterOrder.RATE_LIMIT; }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        String key = resolveKey(context);
        int limit = resolveLimit(context.getPath());
        RateLimiter limiter = getOrCreate(key, limit);

        if (limiter.acquirePermission()) {
            chain.next(context);
        } else {
            log.warn("[{}] Rate limit exceeded: key={}", context.getRequestId(), key);
            context.addResponseHeader("Retry-After", "60");
            context.addResponseHeader("X-RateLimit-Limit", String.valueOf(limit));
            context.abort(429, "Rate limit exceeded");
        }
    }

    private String resolveKey(GatewayContext ctx) {
        if (ctx.getAuthenticatedSubject() != null) return ctx.getAuthenticatedSubject();
        String forwarded = ctx.getHeader("X-Forwarded-For");
        if (forwarded != null) return forwarded.split(",")[0].trim();
        return "anonymous";
    }

    private int resolveLimit(String path) {
        return config.getPathLimits().entrySet().stream()
                .filter(e -> path.startsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(config.getDefaultRpm());
    }

    private RateLimiter getOrCreate(String key, int limit) {
        return limiters.computeIfAbsent(key, k -> RateLimiter.of(k,
                RateLimiterConfig.custom()
                        .limitForPeriod(limit)
                        .limitRefreshPeriod(Duration.ofMinutes(1))
                        .timeoutDuration(Duration.ZERO)
                        .build()));
    }
}

