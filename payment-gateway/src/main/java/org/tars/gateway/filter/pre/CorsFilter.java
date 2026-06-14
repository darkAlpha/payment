package org.tars.gateway.filter.pre;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;

/**
 * CORS filter - handles preflight OPTIONS and sets CORS headers.
 */
public class CorsFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(CorsFilter.class);
    private final GatewayConfig.CorsConfig config;

    public CorsFilter(GatewayConfig.CorsConfig config) {
        this.config = config;
    }

    @Override
    public String name() {
        return "cors";
    }

    @Override
    public int order() {
        return FilterOrder.CORS;
    }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        if (!config.isEnabled()) {
            chain.next(context);
            return;
        }

        String origin = context.getHeader("Origin");
        if (origin != null) {
            if (isOriginAllowed(origin)) {
                context.addResponseHeader("Access-Control-Allow-Origin", origin);
            } else {
                context.addResponseHeader("Access-Control-Allow-Origin", config.getAllowedOrigins().getFirst());
            }
            context.addResponseHeader("Access-Control-Allow-Methods", String.join(", ", config.getAllowedMethods()));
            context.addResponseHeader("Access-Control-Allow-Headers", String.join(", ", config.getAllowedHeaders()));
            context.addResponseHeader("Access-Control-Max-Age", String.valueOf(config.getMaxAge()));

            if (config.isAllowCredentials()) {
                context.addResponseHeader("Access-Control-Allow-Credentials", "true");
            }
        }

        // Handle preflight
        if ("OPTIONS".equalsIgnoreCase(context.getMethod())) {
            context.setResponseStatus(204);
            context.setResponseBody(new byte[0]);
            context.abort(204, "CORS preflight");
            return;
        }

        chain.next(context);
    }

    private boolean isOriginAllowed(String origin) {
        return config.getAllowedOrigins().contains("*") || config.getAllowedOrigins().contains(origin);
    }
}

