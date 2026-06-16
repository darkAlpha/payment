package org.tars.gateway.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.tars.gateway.config.GatewayProperties;
import org.tars.gateway.feature.FeatureFlagService;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.health.HealthService;
import org.tars.gateway.metrics.MetricsService;
import org.tars.gateway.route.RouteRegistry;

import java.util.List;

@Slf4j
@Component
public class NettyGatewayServer {

    private final GatewayProperties props;
    private final List<GatewayFilter> filters;
    private final RouteRegistry routeRegistry;
    private final FeatureFlagService featureFlagService;
    private final MetricsService metricsService;
    private final HealthService healthService;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public NettyGatewayServer(GatewayProperties props,
                              List<GatewayFilter> filters,
                              RouteRegistry routeRegistry,
                              FeatureFlagService featureFlagService,
                              MetricsService metricsService,
                              HealthService healthService) {
        this.props = props;
        this.filters = filters;
        this.routeRegistry = routeRegistry;
        this.featureFlagService = featureFlagService;
        this.metricsService = metricsService;
        this.healthService = healthService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        int port = props.getServer().getPort();
        bossGroup = new NioEventLoopGroup(props.getServer().getBossThreads());
        workerGroup = props.getServer().getWorkerThreads() > 0
                ? new NioEventLoopGroup(props.getServer().getWorkerThreads())
                : new NioEventLoopGroup();

        new Thread(() -> {
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new GatewayChannelInitializer(props, filters, routeRegistry,
                                featureFlagService, metricsService, healthService))
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true);

                serverChannel = b.bind(port).sync().channel();
                log.info("Gateway Netty server started on port {}", port);
                serverChannel.closeFuture().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "gateway-netty").start();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down gateway server...");
        if (serverChannel != null) serverChannel.close();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }
}
