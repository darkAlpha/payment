package org.tars.gateway.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.feature.FeatureFlagManager;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.route.RouteRegistry;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Channel initializer for the gateway server.
 * Sets up the Netty pipeline with HTTP codecs and the gateway request handler.
 */
public class GatewayServerInitializer extends ChannelInitializer<SocketChannel> {

    private final GatewayConfig config;
    private final RouteRegistry routeRegistry;
    private final List<GatewayFilter> filters;
    private final FeatureFlagManager featureFlagManager;

    public GatewayServerInitializer(GatewayConfig config, RouteRegistry routeRegistry,
                                     List<GatewayFilter> filters, FeatureFlagManager featureFlagManager) {
        this.config = config;
        this.routeRegistry = routeRegistry;
        this.filters = filters;
        this.featureFlagManager = featureFlagManager;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // Idle state detection
        pipeline.addLast("idle", new IdleStateHandler(
                config.getServer().getIdleTimeoutSeconds(),
                config.getServer().getIdleTimeoutSeconds(),
                config.getServer().getIdleTimeoutSeconds(),
                TimeUnit.SECONDS));

        // HTTP codec
        pipeline.addLast("codec", new HttpServerCodec());

        // Aggregate HTTP messages
        pipeline.addLast("aggregator", new HttpObjectAggregator(config.getServer().getMaxContentLength()));

        // Gateway request handler
        pipeline.addLast("handler", new GatewayRequestHandler(routeRegistry, filters, config, featureFlagManager));
    }
}

