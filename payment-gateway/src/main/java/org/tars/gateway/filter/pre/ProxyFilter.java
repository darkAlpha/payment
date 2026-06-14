package org.tars.gateway.filter.pre;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;
import org.tars.gateway.proxy.ProxyClient;
import org.tars.gateway.proxy.ProxyResponse;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proxy filter with circuit breaker - forwards requests to upstream services.
 * Implements retry logic and circuit breaking for resilience.
 */
public class ProxyFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(ProxyFilter.class);

    private final ProxyClient proxyClient;
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    public ProxyFilter(ProxyClient proxyClient) {
        this.proxyClient = proxyClient;
    }

    @Override
    public String name() {
        return "proxy";
    }

    @Override
    public int order() {
        return FilterOrder.PROXY;
    }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        if (context.getResolvedUpstream() == null) {
            context.abort(503, "No upstream resolved");
            return;
        }

        GatewayConfig.RouteConfig route = context.getAttribute("matched-route");
        int timeoutMs = route != null ? route.getTimeoutMs() : 30000;
        int retries = route != null ? route.getRetries() : 3;

        // Get or create circuit breaker
        String routeId = route != null ? route.getId() : "default";
        CircuitBreaker circuitBreaker = getCircuitBreaker(routeId, route);

        // Execute with circuit breaker
        ProxyResponse response = null;
        Exception lastException = null;

        for (int attempt = 0; attempt <= retries; attempt++) {
            if (!circuitBreaker.tryAcquirePermission()) {
                log.warn("Circuit breaker OPEN for route: {}", routeId);
                context.abort(503, "Service temporarily unavailable (circuit breaker open)");
                return;
            }

            try {
                long start = System.nanoTime();
                response = proxyClient.forward(context, timeoutMs);
                long durationNanos = System.nanoTime() - start;

                if (response.isServerError() && attempt < retries) {
                    circuitBreaker.onError(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS,
                            new RuntimeException("Server error: " + response.statusCode()));
                    log.warn("Upstream returned {}, retrying ({}/{})", response.statusCode(), attempt + 1, retries);
                    sleep(calculateBackoff(attempt));
                    continue;
                }

                circuitBreaker.onSuccess(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
                break;

            } catch (Exception e) {
                lastException = e;
                circuitBreaker.onError(0, java.util.concurrent.TimeUnit.NANOSECONDS, e);
                if (attempt < retries) {
                    log.warn("Proxy error (attempt {}/{}): {}", attempt + 1, retries, e.getMessage());
                    sleep(calculateBackoff(attempt));
                }
            }
        }

        if (response != null) {
            context.setResponseStatus(response.statusCode());
            context.setResponseBody(response.body());
            if (response.headers() != null) {
                response.headers().forEach(context::addResponseHeader);
            }
        } else {
            String errorMsg = lastException != null ? lastException.getMessage() : "Unknown proxy error";
            context.abort(502, "Bad gateway: " + errorMsg);
        }

        chain.next(context);
    }

    private CircuitBreaker getCircuitBreaker(String routeId, GatewayConfig.RouteConfig route) {
        return circuitBreakers.computeIfAbsent(routeId, id -> {
            CircuitBreakerConfig.Builder configBuilder = CircuitBreakerConfig.custom()
                    .slidingWindowSize(10)
                    .failureRateThreshold(50)
                    .waitDurationInOpenState(Duration.ofSeconds(60));

            if (route != null && route.getCircuitBreaker() != null) {
                GatewayConfig.CircuitBreakerConfig cbConfig = route.getCircuitBreaker();
                configBuilder
                        .slidingWindowSize(cbConfig.getSlidingWindowSize())
                        .failureRateThreshold(cbConfig.getFailureRateThreshold())
                        .waitDurationInOpenState(Duration.ofMillis(cbConfig.getWaitDurationInOpenState()));
            }

            return CircuitBreaker.of(id, configBuilder.build());
        });
    }

    private long calculateBackoff(int attempt) {
        return (long) Math.min(1000 * Math.pow(2, attempt), 10000);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
