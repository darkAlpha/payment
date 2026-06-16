package org.tars.gateway.versioning;

import org.tars.gateway.config.GatewayProperties;
import org.tars.gateway.context.GatewayContext;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class PercentageVersionStrategy implements VersionStrategy {
    @Override
    public String resolve(GatewayContext context, GatewayProperties.Versioning config) {
        Map<String, Integer> pcts = config.getPercentages();
        if (pcts == null || pcts.isEmpty()) return null;
        int total = pcts.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) return null;
        int roll = ThreadLocalRandom.current().nextInt(total);
        int cum = 0;
        for (var entry : pcts.entrySet()) {
            cum += entry.getValue();
            if (roll < cum) return entry.getKey();
        }
        return pcts.keySet().iterator().next();
    }
}
