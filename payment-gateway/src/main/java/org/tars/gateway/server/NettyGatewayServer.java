package org.tars.gateway.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.feature.FeatureFlagManager;
import org.tars.gateway.filter.GatewayFilter;
import org.tars.gateway.filter.pre.*;
import org.tars.gateway.filter.post.AccessLogFilter;
import org.tars.gateway.proxy.ProxyClient;
import org.tars.gateway.route.RouteRegistry;
import org.tars.gateway.security.rbac.RbacManager;
import org.tars.gateway.versioning.VersionRouter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Netty-based API Gateway server.
 * High-performance non-blocking gateway for payment services.
 */
public class NettyGatewayServer {

    private static final Logger log = LoggerFactory.getLogger(NettyGatewayServer.class);

    private final GatewayConfig config;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private ProxyClient proxyClient;

    public NettyGatewayServer(GatewayConfig config) {
        this.config = config;
    }

    public void start() throws InterruptedException {
        int port = config.getServer().getPort();
        int bossThreads = config.getServer().getBossThreads();
        int workerThreads = config.getServer().getWorkerThreads();

        bossGroup = new NioEventLoopGroup(bossThreads);
        workerGroup = workerThreads > 0 ? new NioEventLoopGroup(workerThreads) : new NioEventLoopGroup();

        // Initialize components
        RouteRegistry routeRegistry = new RouteRegistry(config.getRoutes());
        RbacManager rbacManager = new RbacManager();
        FeatureFlagManager featureFlagManager = new FeatureFlagManager(config.getFeatureFlags());
        VersionRouter versionRouter = new VersionRouter();
        proxyClient = new ProxyClient(30000);

        // Build filter chain
        List<GatewayFilter> filters = buildFilterChain(rbacManager, featureFlagManager, versionRouter);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new GatewayServerInitializer(
                            config, routeRegistry, filters, featureFlagManager))
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            serverChannel = bootstrap.bind(port).sync().channel();

            log.info("╔══════════════════════════════════════════════════════╗");
            log.info("║    Payment Gateway started on port {}            ║", String.format("%-5d", port));
            log.info("║    Routes: {}                                       ║", String.format("%-3d", config.getRoutes().size()));
            log.info("║    Filters: {}                                      ║", String.format("%-3d", filters.size()));
            log.info("║    Security: JWT={}, ApiKey={}                 ║",
                    config.getSecurity().getJwt().isEnabled(),
                    config.getSecurity().getApiKey().isEnabled());
            log.info("╚══════════════════════════════════════════════════════╝");

            serverChannel.closeFuture().sync();
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        log.info("Shutting down gateway server...");
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (proxyClient != null) {
            proxyClient.shutdown();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("Gateway server stopped.");
    }

    private List<GatewayFilter> buildFilterChain(
            RbacManager rbacManager,
            FeatureFlagManager featureFlagManager,
            VersionRouter versionRouter) {

        List<GatewayFilter> filters = new ArrayList<>();

        // Pre-filters (ordered by priority)
        filters.add(new AccessLogFilter(config.getServer().isEnableAccessLog()));
        filters.add(new CorsFilter(config.getSecurity().getCors()));
        filters.add(new RequestIdFilter());
        filters.add(new RateLimitFilter(config.getRateLimit()));
        filters.add(new AuthenticationFilter(config.getSecurity()));
        filters.add(new AuthorizationFilter(rbacManager));
        filters.add(new FeatureFlagFilter(featureFlagManager));
        filters.add(new RouteResolveFilter(versionRouter));
        filters.add(new ProxyFilter(proxyClient));

        // Sort by order
        filters.sort(Comparator.comparingInt(GatewayFilter::order));

        log.info("Filter chain built: {}", filters.stream().map(GatewayFilter::name).toList());
        return filters;
    }
}

