package org.tars.gateway.filter;

import org.tars.gateway.context.GatewayContext;

/**
 * Gateway filter interface - implements Chain of Responsibility pattern.
 * Each filter can inspect/modify the request before and after proxying.
 */
public interface GatewayFilter {

    /**
     * Filter name for identification and ordering.
     */
    String name();

    /**
     * Filter execution order. Lower values execute first.
     */
    int order();

    /**
     * Execute filter logic. Call chain.next() to continue processing.
     * Modify context to abort the request.
     */
    void filter(GatewayContext context, GatewayFilterChain chain);
}

