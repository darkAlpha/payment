package org.tars.gateway.filter.pre;

import org.springframework.stereotype.Component;
import org.tars.gateway.config.GatewayProperties;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;

/**
 * CORS pre-flight and header filter.
 */
@Component
public class CorsFilter implements GatewayFilter {

    private final GatewayProperties.Cors config;

    public CorsFilter(GatewayProperties properties) {
        this.config = properties.getSecurity().getCors();
    }

    @Override public String getName() { return "cors"; }
    @Override public int getOrder() { return FilterOrder.CORS; }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        if (!config.isEnabled()) { chain.next(context); return; }

        String origin = context.getHeader("Origin");
        if (origin != null) {
            String allowedOrigin = isOriginAllowed(origin) ? origin : config.getAllowedOrigins().getFirst();
            context.addResponseHeader("Access-Control-Allow-Origin", allowedOrigin);
            context.addResponseHeader("Access-Control-Allow-Methods", String.join(", ", config.getAllowedMethods()));
            context.addResponseHeader("Access-Control-Allow-Headers", String.join(", ", config.getAllowedHeaders()));
            context.addResponseHeader("Access-Control-Max-Age", String.valueOf(config.getMaxAge()));
            if (config.isAllowCredentials()) {
                context.addResponseHeader("Access-Control-Allow-Credentials", "true");
            }
        }

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

