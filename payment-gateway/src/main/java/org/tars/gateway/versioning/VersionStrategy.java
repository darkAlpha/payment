package org.tars.gateway.versioning;

import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.context.GatewayContext;

import java.util.List;

/**
 * Version routing strategy interface.
 * Determines which version of upstream to route to.
 */
public interface VersionStrategy {

    /**
     * Resolve the target version for this request.
     * Returns the version string or null if no version preference.
     */
    String resolveVersion(GatewayContext context, GatewayConfig.VersioningConfig config);
}

