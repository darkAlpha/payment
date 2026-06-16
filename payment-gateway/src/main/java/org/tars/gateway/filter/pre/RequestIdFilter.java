package org.tars.gateway.filter.pre;

import org.springframework.stereotype.Component;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;

/**
 * Propagates or assigns X-Request-Id for distributed tracing.
 */
@Component
public class RequestIdFilter implements GatewayFilter {

    private static final String HEADER = "X-Request-Id";

    @Override public String getName() { return "request-id"; }
    @Override public int getOrder() { return FilterOrder.REQUEST_ID; }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        String existing = context.getHeader(HEADER);
        if (existing == null || existing.isBlank()) {
            context.getHeaders().put(HEADER, context.getRequestId());
        }
        context.addResponseHeader(HEADER, context.getRequestId());
        chain.next(context);
    }
}

