package org.tars.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.config.GatewayConfigLoader;
import org.tars.gateway.server.NettyGatewayServer;

/**
 * Main entry point for the Payment API Gateway.
 * Bootstraps the Netty server with all configured filters, load balancers, and security.
 */
public class GatewayApplication {

    private static final Logger log = LoggerFactory.getLogger(GatewayApplication.class);

    public static void main(String[] args) {
        log.info("=== Payment Gateway Starting ===");

        try {
            GatewayConfig config = GatewayConfigLoader.load("gateway.yaml");
            log.info("Configuration loaded: port={}, routes={}", config.getServer().getPort(), config.getRoutes().size());

            NettyGatewayServer server = new NettyGatewayServer(config);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutting down gateway...");
                server.shutdown();
            }));

            server.start();
        } catch (Exception e) {
            log.error("Failed to start gateway", e);
            System.exit(1);
        }
    }
}

