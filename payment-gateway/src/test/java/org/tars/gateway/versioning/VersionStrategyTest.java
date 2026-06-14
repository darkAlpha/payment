package org.tars.gateway.versioning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.context.GatewayContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Version Routing Strategies")
class VersionStrategyTest {

    @Nested
    @DisplayName("Header-based versioning")
    class HeaderVersionTest {

        @Test
        @DisplayName("should resolve version from X-API-Version header")
        void shouldResolveFromHeader() {
            HeaderVersionStrategy strategy = new HeaderVersionStrategy();
            GatewayConfig.VersioningConfig config = new GatewayConfig.VersioningConfig();
            config.setHeaderName("X-API-Version");

            GatewayContext context = new GatewayContext("req-1", "GET", "/api/v1/deposits",
                    Map.of("X-API-Version", "v2"), Map.of(), null);

            String version = strategy.resolveVersion(context, config);
            assertThat(version).isEqualTo("v2");
        }

        @Test
        @DisplayName("should return null when header is missing")
        void shouldReturnNullWhenMissing() {
            HeaderVersionStrategy strategy = new HeaderVersionStrategy();
            GatewayConfig.VersioningConfig config = new GatewayConfig.VersioningConfig();
            config.setHeaderName("X-API-Version");

            GatewayContext context = new GatewayContext("req-2", "GET", "/api/v1/deposits",
                    Map.of(), Map.of(), null);

            String version = strategy.resolveVersion(context, config);
            assertThat(version).isNull();
        }
    }

    @Nested
    @DisplayName("Percentage-based versioning")
    class PercentageVersionTest {

        @Test
        @DisplayName("should return version within configured percentages")
        void shouldReturnVersionWithinPercentages() {
            PercentageVersionStrategy strategy = new PercentageVersionStrategy();
            GatewayConfig.VersioningConfig config = new GatewayConfig.VersioningConfig();
            Map<String, Integer> percentages = new LinkedHashMap<>();
            percentages.put("v1", 80);
            percentages.put("v2", 20);
            config.setPercentages(percentages);

            Map<String, Integer> counts = new java.util.HashMap<>();
            int iterations = 10000;
            GatewayContext context = new GatewayContext("req-3", "GET", "/test", Map.of(), Map.of(), null);

            for (int i = 0; i < iterations; i++) {
                String version = strategy.resolveVersion(context, config);
                counts.merge(version, 1, Integer::sum);
            }

            double v1Ratio = (double) counts.getOrDefault("v1", 0) / iterations;
            assertThat(v1Ratio).isBetween(0.7, 0.9);
        }

        @Test
        @DisplayName("should return null for empty percentages")
        void shouldReturnNullForEmpty() {
            PercentageVersionStrategy strategy = new PercentageVersionStrategy();
            GatewayConfig.VersioningConfig config = new GatewayConfig.VersioningConfig();
            config.setPercentages(Map.of());

            GatewayContext context = new GatewayContext("req-4", "GET", "/test", Map.of(), Map.of(), null);
            String version = strategy.resolveVersion(context, config);
            assertThat(version).isNull();
        }
    }
}

