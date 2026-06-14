package org.tars.gateway.versioning;

import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.context.GatewayContext;

/**
 * Header-based version routing.
 * Routes based on explicit version header (e.g., X-API-Version: v2).
 */
public class HeaderVersionStrategy implements VersionStrategy {

    @Override
    public String resolveVersion(GatewayContext context, GatewayConfig.VersioningConfig config) {
        String headerName = config.getHeaderName();
        if (headerName == null) headerName = "X-API-Version";

        return context.getHeader(headerName);
    }
}

