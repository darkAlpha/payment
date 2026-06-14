package org.tars.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.context.GatewayContext;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Security - Authentication Providers")
class AuthenticationProviderTest {

    @Nested
    @DisplayName("JWT Authentication")
    class JwtAuthTest {

        private JwtAuthProvider jwtProvider;

        @BeforeEach
        void setup() {
            GatewayConfig.JwtConfig jwtConfig = new GatewayConfig.JwtConfig();
            jwtConfig.setSecret("test-secret-key-must-be-at-least-256-bits-for-hmac-sha-algorithm");
            jwtConfig.setIssuer("test-issuer");
            jwtConfig.setExpirationMs(3600000);
            jwtProvider = new JwtAuthProvider(jwtConfig);
        }

        @Test
        @DisplayName("should authenticate valid JWT token")
        void shouldAuthenticateValidToken() {
            String token = jwtProvider.generateToken("user-1", Set.of("OPERATOR", "VIEWER"), 3600000);
            GatewayContext context = new GatewayContext("req-1", "GET", "/api/v1/test",
                    Map.of("Authorization", "Bearer " + token), Map.of(), null);

            assertThat(jwtProvider.supports(context)).isTrue();
            Optional<AuthenticationResult> result = jwtProvider.authenticate(context);
            assertThat(result).isPresent();
            assertThat(result.get().subject()).isEqualTo("user-1");
            assertThat(result.get().roles()).containsExactlyInAnyOrder("OPERATOR", "VIEWER");
        }

        @Test
        @DisplayName("should reject expired token")
        void shouldRejectExpiredToken() {
            String token = jwtProvider.generateToken("user-1", Set.of("ADMIN"), -1000);
            GatewayContext context = new GatewayContext("req-2", "GET", "/test",
                    Map.of("Authorization", "Bearer " + token), Map.of(), null);
            Optional<AuthenticationResult> result = jwtProvider.authenticate(context);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should not support missing header")
        void shouldNotSupportMissing() {
            GatewayContext context = new GatewayContext("req-3", "GET", "/test",
                    Map.of(), Map.of(), null);
            assertThat(jwtProvider.supports(context)).isFalse();
        }
    }

    @Nested
    @DisplayName("API Key Authentication")
    class ApiKeyAuthTest {

        private ApiKeyAuthProvider apiKeyProvider;

        @BeforeEach
        void setup() {
            GatewayConfig.ApiKeyConfig config = new GatewayConfig.ApiKeyConfig();
            config.setHeaderName("X-API-Key");
            GatewayConfig.ApiKeyEntry entry = new GatewayConfig.ApiKeyEntry();
            entry.setName("test-service");
            entry.setRoles(java.util.List.of("SERVICE", "OPERATOR"));
            entry.setAllowedPaths(java.util.List.of("/api/v1/**"));
            config.setKeys(Map.of("test-key-12345", entry));
            apiKeyProvider = new ApiKeyAuthProvider(config);
        }

        @Test
        @DisplayName("should authenticate valid API key")
        void shouldAuthenticateValidKey() {
            GatewayContext context = new GatewayContext("req-4", "GET", "/api/v1/deposits",
                    Map.of("X-API-Key", "test-key-12345"), Map.of(), null);
            Optional<AuthenticationResult> result = apiKeyProvider.authenticate(context);
            assertThat(result).isPresent();
            assertThat(result.get().subject()).isEqualTo("test-service");
        }

        @Test
        @DisplayName("should reject invalid API key")
        void shouldRejectInvalidKey() {
            GatewayContext context = new GatewayContext("req-5", "GET", "/api/v1/deposits",
                    Map.of("X-API-Key", "wrong-key"), Map.of(), null);
            Optional<AuthenticationResult> result = apiKeyProvider.authenticate(context);
            assertThat(result).isEmpty();
        }
    }
}

