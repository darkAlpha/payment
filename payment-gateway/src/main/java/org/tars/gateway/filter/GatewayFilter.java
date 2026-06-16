package org.tars.gateway.filter;

import org.tars.gateway.context.GatewayContext;

/**
 * Gateway filter contract following Spring's Ordered pattern.
 * Implementations should be Spring components.
 */
public interface GatewayFilter {

    /** Unique filter name. */
    String getName();

    /** Execution order — lower runs first. */
    int getOrder();

    /** Execute filter logic; call chain.next() to proceed. */
    void filter(GatewayContext context, GatewayFilterChain chain);
}

