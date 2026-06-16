package org.tars.gateway.filter.pre;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tars.gateway.config.GatewayProperties;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;
import org.tars.gateway.proxy.NettyProxyClient;
import org.tars.gateway.proxy.ProxyResponse;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proxy filter — forwards request to upstream with circuit breaker and retry.
 */
@Slf4j
@Component
public class ProxyFilter implements GatewayFilter {

    private final NettyProxyClient proxyClient;
    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    public ProxyFilter(NettyProxyClient proxyClient) {
        this.proxyClient = proxyClient;
    }

    @Override public String getName() { return "proxy"; }
    @Override public int getOrder() { return FilterOrder.PROXY; }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        if (context.getResolvedUpstream() == null) {
            context.abort(503, "No upstream resolved");
            return;
        }

        GatewayProperties.Route route = context.getAttribute("matched-route");
        int timeout = route != null ? route.getTimeoutMs() : 30_000;
        int retries = route != null ? route.getRetries() : 3;
        String routeId = route != null ? route.getId() : "default";
        CircuitBreaker cb = getCircuitBreaker(routeId, route);

        ProxyResponse response = null;
        Exception lastError = null;

        for (int attempt = 0; attempt <= retries; attempt++) {
            if (!cb.tryAcquirePermission()) {
                context.abort(503, "Circuit breaker open for: " + routeId);
                return;
            }
            try {
                long start = System.nanoTime();
                response = proxyClient.forward(context, timeout);
                long elapsed = System.nanoTime() - start;

                if (response.isServerError() && attempt < retries) {
                    cb.onError(elapsed, java.util.concurrent.TimeUnit.NANOSECONDS,
                            new RuntimeException("HTTP " + response.statusCode()));
                    log.warn("[{}] Upstream {} returned {}, retry {}/{}",
                            context.getRequestId(), routeId, response.statusCode(), attempt + 1, retries);
                    backoff(attempt);
                    continue;
                }
                cb.onSuccess(elapsed, java.util.concurrent.TimeUnit.NANOSECONDS);
                break;
            } catch (Exception e) {
                lastError = e;
                cb.onError(0, java.util.concurrent.TimeUnit.NANOSECONDS, e);
                if (attempt < retries) {
                    log.warn("[{}] Proxy error, retry {}/{}: {}", context.getRequestId(), attempt + 1, retries, e.getMessage());
                    backoff(attempt);
                }
            }
        }

        if (response != null) {
            context.setResponseStatus(response.statusCode());
            context.setResponseBody(response.body());
            if (response.headers() != null) response.headers().forEach(context::addResponseHeader);
        } else {
            context.abort(502, "Bad gateway: " + (lastError != null ? lastError.getMessage() : "unknown"));
        }
        chain.next(context);
    }

    private CircuitBreaker getCircuitBreaker(String routeId, GatewayProperties.Route route) {
        return breakers.computeIfAbsent(routeId, id -> {
            var builder = CircuitBreakerConfig.custom()
                    .slidingWindowSize(10)
                    .failureRateThreshold(50)
                    .waitDurationInOpenState(Duration.ofSeconds(60));
            if (route != null && route.getCircuitBreaker() != null) {
                var cfg = route.getCircuitBreaker();
                builder.slidingWindowSize(cfg.getSlidingWindowSize())
                        .failureRateThreshold(cfg.getFailureRateThreshold())
                        .waitDurationInOpenState(Duration.ofMillis(cfg.getWaitDurationInOpenStateMs()));
            }
            return CircuitBreaker.of(id, builder.build());
        });
    }

    private void backoff(int attempt) {
        try { Thread.sleep((long) Math.min(1000 * Math.pow(2, attempt), 10_000)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

