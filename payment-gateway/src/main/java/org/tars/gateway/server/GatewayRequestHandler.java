package org.tars.gateway.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.exception.GatewayErrorHandler;
import org.tars.gateway.exception.GatewayException;
import org.tars.gateway.feature.FeatureFlagManager;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.GatewayFilterChain;
import org.tars.gateway.health.HealthCheckHandler;
import org.tars.gateway.metrics.MetricsCollector;
import org.tars.gateway.route.RouteRegistry;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Main request handler for the gateway.
 * Converts Netty HTTP requests to GatewayContext, executes filter chain, and writes response.
 */
public class GatewayRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(GatewayRequestHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RouteRegistry routeRegistry;
    private final List<GatewayFilter> filters;
    private final GatewayConfig config;
    private final FeatureFlagManager featureFlagManager;
    private final HealthCheckHandler healthCheckHandler;

    public GatewayRequestHandler(RouteRegistry routeRegistry, List<GatewayFilter> filters,
                                  GatewayConfig config, FeatureFlagManager featureFlagManager) {
        this.routeRegistry = routeRegistry;
        this.filters = filters;
        this.config = config;
        this.featureFlagManager = featureFlagManager;
        this.healthCheckHandler = new HealthCheckHandler(routeRegistry, featureFlagManager);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        String method = request.method().name();
        String uri = request.uri();
        String path = extractPath(uri);
        Map<String, String> queryParams = extractQueryParams(uri);

        // Handle internal endpoints
        if ("/health".equals(path) || "/gateway/health".equals(path)) {
            writeJsonResponse(ctx, 200, healthCheckHandler.getHealthStatus());
            return;
        }
        if ("/gateway/metrics".equals(path) && config.getMetrics().isEnabled()) {
            writeJsonResponse(ctx, 200, MetricsCollector.getInstance().getMetrics());
            return;
        }
        if ("/gateway/features".equals(path)) {
            writeJsonResponse(ctx, 200, featureFlagManager.getAllFlags());
            return;
        }
        if ("/gateway/routes".equals(path)) {
            writeJsonResponse(ctx, 200, routeRegistry.getAllRoutes());
            return;
        }

        // Build gateway context
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Map<String, String> headers = extractHeaders(request);
        byte[] body = extractBody(request);

        GatewayContext gatewayContext = new GatewayContext(requestId, method, path, headers, queryParams, body);

        // Match route and set as attribute
        routeRegistry.match(method, path).ifPresent(route -> gatewayContext.setAttribute("matched-route", route));

        // Execute filter chain
        try {
            GatewayFilterChain chain = new GatewayFilterChain(filters);
            chain.execute(gatewayContext);
        } catch (GatewayException e) {
            byte[] errorBody = GatewayErrorHandler.handleError(gatewayContext, e);
            gatewayContext.setResponseBody(errorBody);
        } catch (Exception e) {
            byte[] errorBody = GatewayErrorHandler.handleUnexpectedError(gatewayContext, e);
            gatewayContext.setResponseBody(errorBody);
        }

        // Record metrics
        MetricsCollector.getInstance().recordRequest(path, method, gatewayContext.getResponseStatus(), gatewayContext.getElapsedMs());

        // Write response
        writeResponse(ctx, gatewayContext);
    }

    private void writeResponse(ChannelHandlerContext ctx, GatewayContext context) {
        byte[] body = context.getResponseBody() != null ? context.getResponseBody() : new byte[0];
        ByteBuf content = Unpooled.wrappedBuffer(body);

        HttpResponseStatus status = HttpResponseStatus.valueOf(context.getResponseStatus());
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);

        // Set response headers
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        response.headers().set("X-Request-Id", context.getRequestId());
        response.headers().set("X-Response-Time", context.getElapsedMs() + "ms");

        // Add custom response headers
        context.getResponseHeaders().forEach((key, value) -> response.headers().set(key, value));

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void writeJsonResponse(ChannelHandlerContext ctx, int status, Object data) {
        try {
            byte[] json = MAPPER.writeValueAsBytes(data);
            ByteBuf content = Unpooled.wrappedBuffer(json);
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(status), content);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } catch (Exception e) {
            log.error("Failed to write JSON response", e);
            ByteBuf error = Unpooled.copiedBuffer("{\"error\":\"internal\"}", StandardCharsets.UTF_8);
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR, error);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private String extractPath(String uri) {
        int queryIndex = uri.indexOf('?');
        return queryIndex >= 0 ? uri.substring(0, queryIndex) : uri;
    }

    private Map<String, String> extractQueryParams(String uri) {
        Map<String, String> params = new HashMap<>();
        int queryIndex = uri.indexOf('?');
        if (queryIndex >= 0 && queryIndex < uri.length() - 1) {
            String query = uri.substring(queryIndex + 1);
            for (String pair : query.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    params.put(kv[0], kv[1]);
                }
            }
        }
        return params;
    }

    private Map<String, String> extractHeaders(FullHttpRequest request) {
        Map<String, String> headers = new HashMap<>();
        request.headers().forEach(entry -> headers.put(entry.getKey(), entry.getValue()));
        return headers;
    }

    private byte[] extractBody(FullHttpRequest request) {
        ByteBuf content = request.content();
        if (content.isReadable()) {
            byte[] body = new byte[content.readableBytes()];
            content.readBytes(body);
            return body;
        }
        return null;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            log.debug("Closing idle connection: {}", ctx.channel().remoteAddress());
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Channel exception: {}", cause.getMessage());
        ctx.close();
    }
}

