package org.tars.gateway.filter.post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;

/**
 * Access log filter - logs request/response details for auditing.
 */
public class AccessLogFilter implements GatewayFilter {

    private static final Logger accessLog = LoggerFactory.getLogger("gateway.access");

    private final boolean enabled;

    public AccessLogFilter(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String name() {
        return "access-log";
    }

    @Override
    public int order() {
        return FilterOrder.ACCESS_LOG;
    }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        long startNanos = System.nanoTime();

        // Continue chain
        chain.next(context);

        // Log after response
        if (enabled) {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            accessLog.info("{} | {} {} | {} | {}ms | {} | upstream={}",
                    context.getRequestId(),
                    context.getMethod(),
                    context.getPath(),
                    context.getResponseStatus(),
                    durationMs,
                    context.getAuthenticatedSubject() != null ? context.getAuthenticatedSubject() : "anonymous",
                    context.getResolvedUpstream() != null ? context.getResolvedUpstream() : "N/A");
        }
    }
}

