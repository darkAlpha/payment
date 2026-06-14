package org.tars.gateway.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads gateway configuration from YAML file.
 * Supports classpath and file system loading.
 */
public final class GatewayConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(GatewayConfigLoader.class);
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private GatewayConfigLoader() {}

    public static GatewayConfig load(String configFile) {
        try {
            // Try external file first
            Path externalPath = Path.of(configFile);
            if (Files.exists(externalPath)) {
                log.info("Loading configuration from file: {}", externalPath.toAbsolutePath());
                return YAML_MAPPER.readValue(Files.newInputStream(externalPath), GatewayConfig.class);
            }

            // Fallback to classpath
            InputStream is = GatewayConfigLoader.class.getClassLoader().getResourceAsStream(configFile);
            if (is != null) {
                log.info("Loading configuration from classpath: {}", configFile);
                return YAML_MAPPER.readValue(is, GatewayConfig.class);
            }

            log.warn("Configuration file not found: {}. Using defaults.", configFile);
            return new GatewayConfig();
        } catch (Exception e) {
            log.error("Failed to load configuration: {}. Using defaults.", e.getMessage());
            return new GatewayConfig();
        }
    }
}

