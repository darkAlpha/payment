package org.tars.gateway.loadbalancer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.tars.gateway.config.GatewayProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Load Balancer Strategies")
class LoadBalancerStrategyTest {

    private List<GatewayProperties.Upstream> upstreams(int count) {
        List<GatewayProperties.Upstream> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GatewayProperties.Upstream u = new GatewayProperties.Upstream();
            u.setUrl("http://svc-" + i + ":8080");
            u.setWeight(1);
            u.setHealthy(true);
            list.add(u);
        }
        return list;
    }

    @Nested class RoundRobinTest {
        @Test @DisplayName("distributes evenly")
        void even() {
            var s = new RoundRobinStrategy();
            var ups = upstreams(3);
            Map<String, Integer> counts = new ConcurrentHashMap<>();
            for (int i = 0; i < 300; i++) counts.merge(s.select(ups, null).getUrl(), 1, Integer::sum);
            assertThat(counts.values()).allMatch(c -> c == 100);
        }

        @Test @DisplayName("skips unhealthy")
        void skipsUnhealthy() {
            var s = new RoundRobinStrategy();
            var ups = upstreams(3);
            ups.get(1).setHealthy(false);
            for (int i = 0; i < 100; i++) assertThat(s.select(ups, null).getUrl()).isNotEqualTo("http://svc-1:8080");
        }

        @Test @DisplayName("throws when none healthy")
        void throwsEmpty() {
            var s = new RoundRobinStrategy();
            var ups = upstreams(2);
            ups.forEach(u -> u.setHealthy(false));
            assertThatThrownBy(() -> s.select(ups, null)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested class WeightedTest {
        @Test @DisplayName("respects weights")
        void weights() {
            var s = new WeightedStrategy();
            var ups = upstreams(2);
            ups.get(0).setWeight(8);
            ups.get(1).setWeight(2);
            Map<String, Integer> counts = new ConcurrentHashMap<>();
            for (int i = 0; i < 10000; i++) counts.merge(s.select(ups, null).getUrl(), 1, Integer::sum);
            assertThat((double) counts.get("http://svc-0:8080") / 10000).isBetween(0.7, 0.9);
        }
    }

    @Nested class IpHashTest {
        @Test @DisplayName("consistent for same key")
        void consistent() {
            var s = new IpHashStrategy();
            var ups = upstreams(5);
            var first = s.select(ups, "192.168.1.1");
            for (int i = 0; i < 50; i++) assertThat(s.select(ups, "192.168.1.1").getUrl()).isEqualTo(first.getUrl());
        }
    }
}
