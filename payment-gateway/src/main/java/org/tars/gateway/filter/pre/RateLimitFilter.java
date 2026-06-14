package org.tars.gateway.filter.pre;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter using Resilience4j.
 * Limits requests per client/path based on configuration.
 */
public class RateLimitFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final GatewayConfig.RateLimitConfig config;
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    public RateLimitFilter(GatewayConfig.RateLimitConfig config) {
        this.config = config;
    }

    @Override
    public String name() {
        return "rate-limit";
    }

    @Override
    public int order() {
        return FilterOrder.RATE_LIMIT;
    }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        if (!config.isEnabled()) {
            chain.next(context);
            return;
        }

        String key = resolveKey(context);
        int limit = resolveLimit(context.getPath());
        RateLimiter limiter = getOrCreateLimiter(key, limit);

        if (limiter.acquirePermission()) {
            chain.next(context);
        } else {
            log.warn("Rate limit exceeded for key: {} on path: {}", key, context.getPath());
            context.abort(429, "Rate limit exceeded. Try again later.");
            context.addResponseHeader("Retry-After", "60");
            context.addResponseHeader("X-RateLimit-Limit", String.valueOf(limit));
            context.addResponseHeader("X-RateLimit-Remaining", "0");
        }
    }

    private String resolveKey(GatewayContext context) {
        // Use authenticated subject if available, otherwise use a general key
        String subject = context.getAuthenticatedSubject();
        if (subject != null) return subject;

        String clientIp = context.getHeader("X-Forwarded-For");
        if (clientIp != null) return clientIp.split(",")[0].trim();

        return "anonymous";
    }

    private int resolveLimit(String path) {
        // Check path-specific limits
        for (Map.Entry<String, Integer> entry : config.getPathLimits().entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return config.getDefaultRpm();
    }

    private RateLimiter getOrCreateLimiter(String key, int limit) {
        return limiters.computeIfAbsent(key, k -> {
            RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
                    .limitForPeriod(limit)
                    .limitRefreshPeriod(Duration.ofMinutes(1))
                    .timeoutDuration(Duration.ZERO)
                    .build();
            return RateLimiter.of(k, rateLimiterConfig);
        });
    }
}

