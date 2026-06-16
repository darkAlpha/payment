package org.tars.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tars.gateway.context.GatewayContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Gateway Filter Chain")
class GatewayFilterChainTest {

    @Test @DisplayName("executes filters in order")
    void order() {
        List<String> log = new ArrayList<>();
        var chain = new GatewayFilterChain(List.of(
                filter("A", 1, log), filter("B", 2, log), filter("C", 3, log)));
        chain.execute(new GatewayContext("t1", "GET", "/", Map.of(), Map.of(), null));
        assertThat(log).containsExactly("A", "B", "C");
    }

    @Test @DisplayName("stops on abort")
    void abort() {
        List<String> log = new ArrayList<>();
        GatewayFilter abortFilter = new GatewayFilter() {
            public String getName() { return "abort"; }
            public int getOrder() { return 2; }
            public void filter(GatewayContext c, GatewayFilterChain ch) { log.add("abort"); c.abort(403, "no"); ch.next(c); }
        };
        var chain = new GatewayFilterChain(List.of(filter("A", 1, log), abortFilter, filter("C", 3, log)));
        var ctx = new GatewayContext("t2", "GET", "/", Map.of(), Map.of(), null);
        chain.execute(ctx);
        assertThat(log).containsExactly("A", "abort");
        assertThat(ctx.isAborted()).isTrue();
    }

    private GatewayFilter filter(String name, int order, List<String> log) {
        return new GatewayFilter() {
            public String getName() { return name; }
            public int getOrder() { return order; }
            public void filter(GatewayContext c, GatewayFilterChain ch) { log.add(name); ch.next(c); }
        };
    }
}
