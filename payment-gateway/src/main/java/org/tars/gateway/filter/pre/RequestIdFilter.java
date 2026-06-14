package org.tars.gateway.filter.pre;

import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;

import java.util.UUID;

/**
 * Assigns a unique request ID to each incoming request.
 * Propagates X-Request-Id header for distributed tracing.
 */
public class RequestIdFilter implements GatewayFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public String name() {
        return "request-id";
    }

    @Override
    public int order() {
        return FilterOrder.REQUEST_ID;
    }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        // Use existing request ID if provided, otherwise use the one from context
        String existingId = context.getHeader(REQUEST_ID_HEADER);
        if (existingId == null || existingId.isBlank()) {
            context.getHeaders().put(REQUEST_ID_HEADER, context.getRequestId());
        }
        context.addResponseHeader(REQUEST_ID_HEADER, context.getRequestId());

        chain.next(context);
    }
}

