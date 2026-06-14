package org.tars.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.context.GatewayContext;

import java.util.List;

/**
 * Executes filters in order, allowing each filter to halt processing.
 */
public class GatewayFilterChain {

    private static final Logger log = LoggerFactory.getLogger(GatewayFilterChain.class);

    private final List<GatewayFilter> filters;
    private int currentIndex = 0;

    public GatewayFilterChain(List<GatewayFilter> filters) {
        this.filters = filters;
    }

    /**
     * Proceed to the next filter in the chain.
     */
    public void next(GatewayContext context) {
        if (context.isAborted()) {
            log.debug("Request {} aborted at filter index {}: {}",
                    context.getRequestId(), currentIndex - 1, context.getAbortReason());
            return;
        }

        if (currentIndex >= filters.size()) {
            return; // All filters executed
        }

        GatewayFilter filter = filters.get(currentIndex++);
        log.trace("Executing filter: {} (order={})", filter.name(), filter.order());
        filter.filter(context, this);
    }

    /**
     * Start chain execution from the beginning.
     */
    public void execute(GatewayContext context) {
        this.currentIndex = 0;
        next(context);
    }
}

