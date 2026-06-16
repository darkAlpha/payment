package org.tars.gateway.versioning;

import org.tars.gateway.config.GatewayProperties;
import org.tars.gateway.context.GatewayContext;

public interface VersionStrategy {
    String resolve(GatewayContext context, GatewayProperties.Versioning config);
}
