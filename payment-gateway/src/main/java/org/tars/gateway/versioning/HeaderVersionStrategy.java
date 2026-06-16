package org.tars.gateway.versioning;

import org.tars.gateway.config.GatewayProperties;
import org.tars.gateway.context.GatewayContext;

public class HeaderVersionStrategy implements VersionStrategy {
    @Override
    public String resolve(GatewayContext context, GatewayProperties.Versioning config) {
        return context.getHeader(config.getHeaderName() != null ? config.getHeaderName() : "X-API-Version");
    }
}
