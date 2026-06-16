package org.tars.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.tars.gateway.config.GatewaySecurityProperties;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final GatewaySecurityProperties props;

    public SecurityConfig(JwtService jwtService, GatewaySecurityProperties props) {
        this.jwtService = jwtService;
        this.props = props;
    }

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        String[] publicPaths = props.getPublicPaths().toArray(String[]::new);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsSource()))
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers(publicPaths).permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(jwtAuthFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                        .accessDeniedHandler((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        })
                )
                .build();
    }

    private AuthenticationWebFilter jwtAuthFilter() {
        ReactiveAuthenticationManager authManager = authentication -> {
            String credentials = authentication.getCredentials().toString();
            String type = authentication.getPrincipal().toString();

            if ("JWT".equals(type)) {
                return jwtService.validate(credentials)
                        .<org.springframework.security.core.Authentication>map(claims -> new UsernamePasswordAuthenticationToken(
                                claims.subject(), null,
                                claims.roles().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList()))
                        .map(Mono::just).orElse(Mono.empty());
            }
            if ("API_KEY".equals(type)) {
                var entry = props.getApiKey().getKeys().get(credentials);
                if (entry != null) {
                    return Mono.just(new UsernamePasswordAuthenticationToken(
                            entry.getName(), null,
                            entry.getRoles().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList()));
                }
            }
            return Mono.empty();
        };

        AuthenticationWebFilter filter = new AuthenticationWebFilter(authManager);
        filter.setServerAuthenticationConverter(tokenConverter());
        return filter;
    }

    private ServerAuthenticationConverter tokenConverter() {
        return exchange -> {
            // Try JWT
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                return Mono.just(new UsernamePasswordAuthenticationToken("JWT", token));
            }
            // Try API key
            String apiKey = exchange.getRequest().getHeaders().getFirst(props.getApiKey().getHeaderName());
            if (apiKey != null && !apiKey.isBlank()) {
                return Mono.just(new UsernamePasswordAuthenticationToken("API_KEY", apiKey));
            }
            return Mono.empty();
        };
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
