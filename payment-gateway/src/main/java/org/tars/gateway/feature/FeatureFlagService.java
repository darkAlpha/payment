package org.tars.gateway.feature;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tars.gateway.config.FeatureFlagProperties;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class FeatureFlagService {

    private final Map<String, FeatureFlagProperties.FeatureEntry> flags = new ConcurrentHashMap<>();
    private final boolean enabled;

    public FeatureFlagService(FeatureFlagProperties props) {
        this.enabled = props.isEnabled();
        this.flags.putAll(props.getFlags());
        log.info("Feature flags: enabled={}, flags={}", enabled, flags.keySet());
    }

    public boolean isEnabled(String name) {
        return isEnabled(name, Set.of());
    }

    public boolean isEnabled(String name, Set<String> roles) {
        if (!enabled) return true;
        var entry = flags.get(name);
        if (entry == null) return true;
        if (!entry.isEnabled()) return false;
        if (!entry.getAllowedRoles().isEmpty() && (roles == null || roles.isEmpty()
                || entry.getAllowedRoles().stream().noneMatch(roles::contains))) return false;
        if (entry.getPercentage() < 100) return ThreadLocalRandom.current().nextInt(100) < entry.getPercentage();
        return true;
    }

    public void toggle(String name, boolean on) {
        var e = flags.get(name);
        if (e != null) { e.setEnabled(on); log.info("Feature '{}' -> {}", name, on); }
    }

    public Map<String, FeatureFlagProperties.FeatureEntry> getAll() { return Map.copyOf(flags); }
}
