package org.tars.gateway.feature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tars.gateway.config.GatewayConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Feature Flag Manager")
class FeatureFlagManagerTest {

    private FeatureFlagManager manager;

    @BeforeEach
    void setup() {
        GatewayConfig.FeatureFlagConfig config = new GatewayConfig.FeatureFlagConfig();
        config.setEnabled(true);

        Map<String, GatewayConfig.FeatureEntry> flags = new HashMap<>();

        GatewayConfig.FeatureEntry enabledFeature = new GatewayConfig.FeatureEntry();
        enabledFeature.setEnabled(true);
        enabledFeature.setPercentage(100);
        flags.put("deposits", enabledFeature);

        GatewayConfig.FeatureEntry disabledFeature = new GatewayConfig.FeatureEntry();
        disabledFeature.setEnabled(false);
        flags.put("new-product", disabledFeature);

        GatewayConfig.FeatureEntry roleFeature = new GatewayConfig.FeatureEntry();
        roleFeature.setEnabled(true);
        roleFeature.setAllowedRoles(List.of("ADMIN"));
        roleFeature.setPercentage(100);
        flags.put("admin-only", roleFeature);

        config.setFlags(flags);
        manager = new FeatureFlagManager(config);
    }

    @Test
    @DisplayName("should return true for enabled feature")
    void shouldReturnTrueForEnabled() {
        assertThat(manager.isEnabled("deposits")).isTrue();
    }

    @Test
    @DisplayName("should return false for disabled feature")
    void shouldReturnFalseForDisabled() {
        assertThat(manager.isEnabled("new-product")).isFalse();
    }

    @Test
    @DisplayName("should check role-based access")
    void shouldCheckRoles() {
        assertThat(manager.isEnabled("admin-only", Set.of("ADMIN"))).isTrue();
        assertThat(manager.isEnabled("admin-only", Set.of("VIEWER"))).isFalse();
        assertThat(manager.isEnabled("admin-only", Set.of())).isFalse();
    }

    @Test
    @DisplayName("should return true for unknown features")
    void shouldReturnTrueForUnknown() {
        assertThat(manager.isEnabled("unknown-feature")).isTrue();
    }

    @Test
    @DisplayName("should support dynamic toggle")
    void shouldToggleDynamically() {
        assertThat(manager.isEnabled("deposits")).isTrue();
        manager.setEnabled("deposits", false);
        assertThat(manager.isEnabled("deposits")).isFalse();
        manager.setEnabled("deposits", true);
        assertThat(manager.isEnabled("deposits")).isTrue();
    }
}

