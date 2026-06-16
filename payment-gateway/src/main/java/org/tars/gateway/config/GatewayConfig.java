package org.tars.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({GatewaySecurityProperties.class, FeatureFlagProperties.class})
public class GatewayConfig {
}
