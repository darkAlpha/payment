package org.tars.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.context.GatewayContext;

import java.util.List;

/**
 * Executes an ordered list of filters as a chain (Chain of Responsibility).
 * Stops execution if context is aborted.
 */
public class GatewayFilterChain {

    private static final Logger log = LoggerFactory.getLogger(GatewayFilterChain.class);

    private final List<GatewayFilter> filters;
    private int index;

    public GatewayFilterChain(List<GatewayFilter> filters) {
        this.filters = filters;
    }

    public void next(GatewayContext context) {
        if (context.isAborted()) {
            log.debug("[{}] Chain aborted: {}", context.getRequestId(), context.getAbortReason());
            return;
        }
        if (index >= filters.size()) {
            return;
        }
        GatewayFilter filter = filters.get(index++);
        log.trace("[{}] Executing filter: {}", context.getRequestId(), filter.getName());
        filter.filter(context, this);
    }

    public void execute(GatewayContext context) {
        this.index = 0;
        next(context);
    }
}

