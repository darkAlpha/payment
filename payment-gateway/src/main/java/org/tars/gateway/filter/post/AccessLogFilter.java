package org.tars.gateway.filter.post;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tars.gateway.config.GatewayProperties;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;
import org.tars.gateway.metrics.MetricsService;

/**
 * Access log filter — logs request lifecycle.
 */
@Slf4j
@Component
public class AccessLogFilter implements GatewayFilter {

    private static final org.slf4j.Logger ACCESS = org.slf4j.LoggerFactory.getLogger("gateway.access");
    private final boolean enabled;

    public AccessLogFilter(GatewayProperties properties) {
        this.enabled = properties.getServer().isAccessLogEnabled();
    }

    @Override public String getName() { return "access-log"; }
    @Override public int getOrder() { return FilterOrder.ACCESS_LOG; }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        chain.next(context);

        if (enabled) {
            ACCESS.info("{} | {} {} | {} | {}ms | {} | upstream={}",
                    context.getRequestId(),
                    context.getMethod(), context.getPath(),
                    context.getResponseStatus(),
                    context.getElapsedMs(),
                    context.getAuthenticatedSubject() != null ? context.getAuthenticatedSubject() : "anon",
                    context.getResolvedUpstream() != null ? context.getResolvedUpstream() : "-");
        }
    }
}

