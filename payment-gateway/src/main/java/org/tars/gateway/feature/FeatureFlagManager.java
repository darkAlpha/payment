package org.tars.gateway.feature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.config.GatewayConfig;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Feature flag manager.
 * Controls feature availability based on configuration, roles, and rollout percentage.
 */
public class FeatureFlagManager {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagManager.class);

    private final Map<String, GatewayConfig.FeatureEntry> flags = new ConcurrentHashMap<>();
    private final boolean enabled;

    public FeatureFlagManager(GatewayConfig.FeatureFlagConfig config) {
        this.enabled = config.isEnabled();
        this.flags.putAll(config.getFlags());
        log.info("Feature flag manager initialized: enabled={}, flags={}", enabled, flags.keySet());
    }

    /**
     * Check if a feature is enabled for the given context.
     */
    public boolean isEnabled(String featureName, Set<String> userRoles) {
        if (!enabled) return true; // If feature flags disabled, everything is enabled

        GatewayConfig.FeatureEntry entry = flags.get(featureName);
        if (entry == null) return true; // Unknown features are enabled by default

        if (!entry.isEnabled()) return false;

        // Check role-based access
        if (!entry.getAllowedRoles().isEmpty()) {
            if (userRoles == null || userRoles.isEmpty()) return false;
            boolean hasRole = entry.getAllowedRoles().stream().anyMatch(userRoles::contains);
            if (!hasRole) return false;
        }

        // Check percentage rollout
        if (entry.getPercentage() < 100) {
            int roll = ThreadLocalRandom.current().nextInt(100);
            return roll < entry.getPercentage();
        }

        return true;
    }

    /**
     * Check if a feature is enabled (without role check).
     */
    public boolean isEnabled(String featureName) {
        return isEnabled(featureName, Set.of());
    }

    /**
     * Dynamically toggle a feature flag at runtime.
     */
    public void setEnabled(String featureName, boolean enabled) {
        GatewayConfig.FeatureEntry entry = flags.get(featureName);
        if (entry != null) {
            entry.setEnabled(enabled);
            log.info("Feature flag '{}' set to: {}", featureName, enabled);
        }
    }

    /**
     * Register a new feature flag.
     */
    public void register(String name, GatewayConfig.FeatureEntry entry) {
        flags.put(name, entry);
        log.info("Feature flag registered: {}", name);
    }

    public Map<String, GatewayConfig.FeatureEntry> getAllFlags() {
        return Map.copyOf(flags);
    }
}

