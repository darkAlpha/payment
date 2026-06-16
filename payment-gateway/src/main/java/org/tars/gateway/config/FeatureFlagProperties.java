package org.tars.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.*;

@Data
@ConfigurationProperties(prefix = "gateway.feature-flags")
public class FeatureFlagProperties {
    private boolean enabled = true;
    private Map<String, FeatureEntry> flags = new LinkedHashMap<>();

    @Data
    public static class FeatureEntry {
        private boolean enabled = true;
        private String description = "";
        private List<String> allowedRoles = new ArrayList<>();
        private int percentage = 100;
    }
}
