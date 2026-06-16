package org.tars.gateway.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.feature.FeatureFlagService;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;
import org.tars.gateway.health.HealthService;
import org.tars.gateway.metrics.MetricsService;
import org.tars.gateway.route.RouteRegistry;

import java.util.*;

@Slf4j
public class GatewayRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final List<GatewayFilter> filters;
    private final RouteRegistry routeRegistry;
    private final FeatureFlagService featureFlagService;
    private final MetricsService metricsService;
    private final HealthService healthService;

    public GatewayRequestHandler(List<GatewayFilter> filters, RouteRegistry routeRegistry,
                                  FeatureFlagService featureFlagService, MetricsService metricsService,
                                  HealthService healthService) {
        this.filters = filters;
        this.routeRegistry = routeRegistry;
        this.featureFlagService = featureFlagService;
        this.metricsService = metricsService;
        this.healthService = healthService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        String path = uri.contains("?") ? uri.substring(0, uri.indexOf('?')) : uri;
        String method = request.method().name();

        // Internal endpoints
        if ("/health".equals(path) || "/gateway/health".equals(path)) { json(ctx, 200, healthService.status()); return; }
        if ("/gateway/metrics".equals(path)) { json(ctx, 200, metricsService.snapshot()); return; }
        if ("/gateway/features".equals(path)) { json(ctx, 200, featureFlagService.getAll()); return; }
        if ("/gateway/routes".equals(path)) { json(ctx, 200, routeRegistry.getAll()); return; }

        // Build context
        String requestId = UUID.randomUUID().toString().substring(0, 16).replace("-", "");
        Map<String, String> headers = new LinkedHashMap<>();
        request.headers().forEach(e -> headers.put(e.getKey(), e.getValue()));
        Map<String, String> params = parseQuery(uri);
        byte[] body = extractBody(request);

        GatewayContext gatewayCtx = new GatewayContext(requestId, method, path, headers, params, body);

        // Execute filter chain
        try {
            new GatewayFilterChain(filters).execute(gatewayCtx);
        } catch (Exception e) {
            log.error("[{}] Unhandled error: {}", requestId, e.getMessage(), e);
            gatewayCtx.setResponseStatus(500);
            gatewayCtx.setResponseBody(("{\"error\":\"INTERNAL\",\"message\":\"" + e.getMessage() + "\"}").getBytes());
        }

        metricsService.record(path, method, gatewayCtx.getResponseStatus(), gatewayCtx.getElapsedMs());
        writeResponse(ctx, gatewayCtx);
    }

    private void writeResponse(ChannelHandlerContext ctx, GatewayContext gc) {
        byte[] body = gc.getResponseBody() != null ? gc.getResponseBody() : new byte[0];
        FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(gc.getResponseStatus()), Unpooled.wrappedBuffer(body));
        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        resp.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.length);
        resp.headers().set("X-Request-Id", gc.getRequestId());
        resp.headers().set("X-Response-Time", gc.getElapsedMs() + "ms");
        gc.getResponseHeaders().forEach((k, v) -> resp.headers().set(k, v));
        ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }

    private void json(ChannelHandlerContext ctx, int status, Object data) {
        try {
            byte[] json = MAPPER.writeValueAsBytes(data);
            FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.valueOf(status), Unpooled.wrappedBuffer(json));
            resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            resp.headers().set(HttpHeaderNames.CONTENT_LENGTH, json.length);
            ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
        } catch (Exception e) {
            ctx.close();
        }
    }

    private Map<String, String> parseQuery(String uri) {
        Map<String, String> p = new HashMap<>();
        int idx = uri.indexOf('?');
        if (idx >= 0 && idx < uri.length() - 1) {
            for (String pair : uri.substring(idx + 1).split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) p.put(kv[0], kv[1]);
            }
        }
        return p;
    }

    private byte[] extractBody(FullHttpRequest req) {
        ByteBuf content = req.content();
        if (content.isReadable()) { byte[] b = new byte[content.readableBytes()]; content.readBytes(b); return b; }
        return null;
    }

    @Override public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) ctx.close();
    }

    @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Channel error: {}", cause.getMessage());
        ctx.close();
    }
}
