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

    @Test
    @DisplayName("should execute filters in order")
    void shouldExecuteInOrder() {
        List<String> executionOrder = new ArrayList<>();

        GatewayFilter filter1 = createFilter("first", 100, executionOrder);
        GatewayFilter filter2 = createFilter("second", 200, executionOrder);
        GatewayFilter filter3 = createFilter("third", 300, executionOrder);

        GatewayContext context = new GatewayContext("test-1", "GET", "/test", Map.of(), Map.of(), null);
        GatewayFilterChain chain = new GatewayFilterChain(List.of(filter1, filter2, filter3));
        chain.execute(context);

        assertThat(executionOrder).containsExactly("first", "second", "third");
    }

    @Test
    @DisplayName("should stop chain when context is aborted")
    void shouldStopOnAbort() {
        List<String> executionOrder = new ArrayList<>();

        GatewayFilter filter1 = createFilter("first", 100, executionOrder);
        GatewayFilter abortFilter = new GatewayFilter() {
            public String name() { return "abort"; }
            public int order() { return 200; }
            public void filter(GatewayContext ctx, GatewayFilterChain c) {
                executionOrder.add("abort");
                ctx.abort(403, "Forbidden");
                c.next(ctx);
            }
        };
        GatewayFilter filter3 = createFilter("third", 300, executionOrder);

        GatewayContext context = new GatewayContext("test-2", "GET", "/test", Map.of(), Map.of(), null);
        GatewayFilterChain chain = new GatewayFilterChain(List.of(filter1, abortFilter, filter3));
        chain.execute(context);

        assertThat(executionOrder).containsExactly("first", "abort");
        assertThat(context.isAborted()).isTrue();
        assertThat(context.getResponseStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("should handle empty filter chain")
    void shouldHandleEmptyChain() {
        GatewayContext context = new GatewayContext("test-3", "GET", "/test", Map.of(), Map.of(), null);
        GatewayFilterChain chain = new GatewayFilterChain(List.of());
        chain.execute(context);
        assertThat(context.isAborted()).isFalse();
    }

    private GatewayFilter createFilter(String filterName, int filterOrder, List<String> log) {
        return new GatewayFilter() {
            public String name() { return filterName; }
            public int order() { return filterOrder; }
            public void filter(GatewayContext ctx, GatewayFilterChain c) {
                log.add(filterName);
                c.next(ctx);
            }
        };
    }
}

