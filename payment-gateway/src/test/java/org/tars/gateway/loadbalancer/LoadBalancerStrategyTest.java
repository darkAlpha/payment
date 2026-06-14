package org.tars.gateway.loadbalancer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.tars.gateway.config.GatewayConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Load Balancer Strategies")
class LoadBalancerStrategyTest {

    private List<GatewayConfig.UpstreamConfig> createUpstreams(int count) {
        List<GatewayConfig.UpstreamConfig> upstreams = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GatewayConfig.UpstreamConfig u = new GatewayConfig.UpstreamConfig();
            u.setUrl("http://service-" + i + ":8080");
            u.setWeight(1);
            u.setHealthy(true);
            upstreams.add(u);
        }
        return upstreams;
    }

    @Nested
    @DisplayName("Round Robin Strategy")
    class RoundRobinTest {

        @Test
        @DisplayName("should distribute evenly across upstreams")
        void shouldDistributeEvenly() {
            RoundRobinStrategy strategy = new RoundRobinStrategy();
            List<GatewayConfig.UpstreamConfig> upstreams = createUpstreams(3);

            Map<String, Integer> counts = new ConcurrentHashMap<>();
            for (int i = 0; i < 300; i++) {
                var selected = strategy.select(upstreams, null);
                counts.merge(selected.getUrl(), 1, Integer::sum);
            }

            assertThat(counts.values()).allMatch(c -> c == 100);
        }

        @Test
        @DisplayName("should skip unhealthy upstreams")
        void shouldSkipUnhealthy() {
            RoundRobinStrategy strategy = new RoundRobinStrategy();
            List<GatewayConfig.UpstreamConfig> upstreams = createUpstreams(3);
            upstreams.get(1).setHealthy(false);

            for (int i = 0; i < 100; i++) {
                var selected = strategy.select(upstreams, null);
                assertThat(selected.getUrl()).isNotEqualTo("http://service-1:8080");
            }
        }

        @Test
        @DisplayName("should throw when no healthy upstreams")
        void shouldThrowWhenNoHealthy() {
            RoundRobinStrategy strategy = new RoundRobinStrategy();
            List<GatewayConfig.UpstreamConfig> upstreams = createUpstreams(2);
            upstreams.forEach(u -> u.setHealthy(false));

            assertThatThrownBy(() -> strategy.select(upstreams, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No healthy upstreams");
        }
    }

    @Nested
    @DisplayName("Weighted Strategy")
    class WeightedTest {

        @Test
        @DisplayName("should respect weights in distribution")
        void shouldRespectWeights() {
            WeightedStrategy strategy = new WeightedStrategy();
            List<GatewayConfig.UpstreamConfig> upstreams = createUpstreams(2);
            upstreams.get(0).setWeight(8); // 80%
            upstreams.get(1).setWeight(2); // 20%

            Map<String, Integer> counts = new ConcurrentHashMap<>();
            int iterations = 10000;
            for (int i = 0; i < iterations; i++) {
                var selected = strategy.select(upstreams, null);
                counts.merge(selected.getUrl(), 1, Integer::sum);
            }

            int service0Count = counts.getOrDefault("http://service-0:8080", 0);
            // Should be roughly 80% ±5%
            assertThat((double) service0Count / iterations).isBetween(0.7, 0.9);
        }
    }

    @Nested
    @DisplayName("Random Strategy")
    class RandomTest {

        @Test
        @DisplayName("should select from healthy upstreams only")
        void shouldSelectHealthyOnly() {
            RandomStrategy strategy = new RandomStrategy();
            List<GatewayConfig.UpstreamConfig> upstreams = createUpstreams(3);
            upstreams.get(2).setHealthy(false);

            for (int i = 0; i < 100; i++) {
                var selected = strategy.select(upstreams, null);
                assertThat(selected.getUrl()).isNotEqualTo("http://service-2:8080");
            }
        }
    }

    @Nested
    @DisplayName("Least Connections Strategy")
    class LeastConnectionsTest {

        @Test
        @DisplayName("should select upstream with fewest connections")
        void shouldSelectLeastConnections() {
            LeastConnectionsStrategy strategy = new LeastConnectionsStrategy();
            List<GatewayConfig.UpstreamConfig> upstreams = createUpstreams(3);

            // First request goes to first (all have 0)
            var first = strategy.select(upstreams, null);
            // Now first has 1 connection, so next should go elsewhere
            var second = strategy.select(upstreams, null);

            assertThat(first.getUrl()).isNotEqualTo(second.getUrl());
        }

        @Test
        @DisplayName("should rebalance after releasing connections")
        void shouldRebalanceAfterRelease() {
            LeastConnectionsStrategy strategy = new LeastConnectionsStrategy();
            List<GatewayConfig.UpstreamConfig> upstreams = createUpstreams(2);

            // Build up connections on service-0
            strategy.select(upstreams, null); // service-0: 1
            strategy.select(upstreams, null); // service-1: 1
            strategy.select(upstreams, null); // service-0: 2

            // Release from service-0
            strategy.releaseConnection("http://service-0:8080");
            strategy.releaseConnection("http://service-0:8080");

            // Now service-0 has 0, service-1 has 1 -> should pick service-0
            var selected = strategy.select(upstreams, null);
            assertThat(selected.getUrl()).isEqualTo("http://service-0:8080");
        }
    }

    @Nested
    @DisplayName("IP Hash Strategy")
    class IpHashTest {

        @Test
        @DisplayName("should consistently route same key to same upstream")
        void shouldBeConsistent() {
            IpHashStrategy strategy = new IpHashStrategy();
            List<GatewayConfig.UpstreamConfig> upstreams = createUpstreams(5);

            String clientIp = "192.168.1.100";
            var first = strategy.select(upstreams, clientIp);

            // Same key should always return same upstream
            for (int i = 0; i < 100; i++) {
                var selected = strategy.select(upstreams, clientIp);
                assertThat(selected.getUrl()).isEqualTo(first.getUrl());
            }
        }

        @Test
        @DisplayName("should distribute different keys across upstreams")
        void shouldDistributeDifferentKeys() {
            IpHashStrategy strategy = new IpHashStrategy();
            List<GatewayConfig.UpstreamConfig> upstreams = createUpstreams(3);

            Map<String, Integer> counts = new ConcurrentHashMap<>();
            for (int i = 0; i < 300; i++) {
                var selected = strategy.select(upstreams, "client-" + i);
                counts.merge(selected.getUrl(), 1, Integer::sum);
            }

            // Should have some distribution (not all to one)
            assertThat(counts.size()).isGreaterThan(1);
        }
    }

    @Nested
    @DisplayName("Load Balancer Factory")
    class FactoryTest {

        @Test
        @DisplayName("should return all registered strategies")
        void shouldReturnAllStrategies() {
            assertThat(LoadBalancerFactory.all()).containsKeys(
                    "round-robin", "weighted", "random", "least-connections", "ip-hash");
        }

        @Test
        @DisplayName("should throw for unknown strategy")
        void shouldThrowForUnknown() {
            assertThatThrownBy(() -> LoadBalancerFactory.get("unknown"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}

