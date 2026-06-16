package org.tars.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.tars.gateway.config.GatewayProperties;

/**
 * Payment API Gateway - Spring Boot Application.
 * <p>
 * Netty-based high-performance gateway with:
 * - Load balancing (round-robin, weighted, random, least-connections, ip-hash)
 * - JWT + API key security with RBAC
 * - Feature flags with percentage rollout
 * - API versioning (header / percentage)
 * - Circuit breaker, rate limiting, retry
 */
@SpringBootApplication
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}

