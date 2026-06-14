package org.tars.gateway.versioning;

import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.context.GatewayContext;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Percentage-based version routing (canary/blue-green deployments).
 * Routes a percentage of traffic to different versions.
 *
 * Example: v1=80%, v2=20% -> 80% traffic goes to v1, 20% to v2
 */
public class PercentageVersionStrategy implements VersionStrategy {

    @Override
    public String resolveVersion(GatewayContext context, GatewayConfig.VersioningConfig config) {
        Map<String, Integer> percentages = config.getPercentages();
        if (percentages == null || percentages.isEmpty()) {
            return null;
        }

        int totalPercentage = percentages.values().stream().mapToInt(Integer::intValue).sum();
        if (totalPercentage <= 0) return null;

        int roll = ThreadLocalRandom.current().nextInt(totalPercentage);
        int cumulative = 0;

        for (Map.Entry<String, Integer> entry : percentages.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) {
                return entry.getKey();
            }
        }

        // Fallback to first version
        return percentages.keySet().iterator().next();
    }
}

