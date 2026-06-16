package org.tars.gateway.filter.pre;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.feature.FeatureFlagService;
import org.tars.gateway.filter.FilterOrder;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;

/**
 * Feature flag filter — blocks disabled features.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "gateway.feature-flags", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FeatureFlagFilter implements GatewayFilter {

    private final FeatureFlagService featureFlagService;

    public FeatureFlagFilter(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @Override public String getName() { return "feature-flag"; }
    @Override public int getOrder() { return FilterOrder.FEATURE_FLAG; }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        String feature = extractFeature(context.getPath());
        if (feature != null && !featureFlagService.isEnabled(feature, context.getRoles())) {
            log.info("[{}] Feature disabled: {}", context.getRequestId(), feature);
            context.abort(404, "Feature not available: " + feature);
            return;
        }
        chain.next(context);
    }

    private String extractFeature(String path) {
        String[] parts = path.split("/");
        // /api/v{N}/{feature}/...
        if (parts.length >= 4 && "api".equals(parts[1])) return parts[3];
        if (parts.length >= 3) return parts[2];
        return null;
    }
}

