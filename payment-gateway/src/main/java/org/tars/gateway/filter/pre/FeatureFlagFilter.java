package org.tars.gateway.filter.pre;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.feature.FeatureFlagManager;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;

/**
 * Feature flag filter - blocks requests to disabled features.
 */
public class FeatureFlagFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagFilter.class);
    private final FeatureFlagManager featureFlagManager;

    public FeatureFlagFilter(FeatureFlagManager featureFlagManager) {
        this.featureFlagManager = featureFlagManager;
    }

    @Override
    public String name() {
        return "feature-flag";
    }

    @Override
    public int order() {
        return FilterOrder.FEATURE_FLAG;
    }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        // Extract feature name from path (first path segment after /api/)
        String featureName = extractFeatureName(context.getPath());
        if (featureName != null) {
            if (!featureFlagManager.isEnabled(featureName, context.getRoles())) {
                log.info("Feature '{}' is disabled. Blocking request {}", featureName, context.getRequestId());
                context.abort(404, "Feature not available: " + featureName);
                return;
            }
        }

        chain.next(context);
    }

    private String extractFeatureName(String path) {
        // Pattern: /api/v{N}/{feature}/**
        String[] segments = path.split("/");
        if (segments.length >= 4 && "api".equals(segments[1])) {
            return segments[3]; // e.g., "deposits", "transfers"
        }
        if (segments.length >= 3) {
            return segments[2];
        }
        return null;
    }
}

