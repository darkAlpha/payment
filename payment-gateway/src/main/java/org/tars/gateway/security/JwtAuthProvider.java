package org.tars.gateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.config.GatewayConfig;
import org.tars.gateway.context.GatewayContext;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JWT-based authentication provider.
 * Validates Bearer tokens from the Authorization header.
 */
public class JwtAuthProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthProvider.class);

    private final GatewayConfig.JwtConfig config;
    private final SecretKey signingKey;
    private final JwtParser jwtParser;

    public JwtAuthProvider(GatewayConfig.JwtConfig config) {
        this.config = config;
        this.signingKey = Keys.hmacShaKeyFor(config.getSecret().getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser()
                .verifyWith(signingKey)
                .build();
    }

    @Override
    public String type() {
        return "JWT";
    }

    @Override
    public boolean supports(GatewayContext context) {
        String authHeader = context.getHeader(config.getHeaderName());
        return authHeader != null && authHeader.startsWith(config.getTokenPrefix());
    }

    @Override
    public Optional<AuthenticationResult> authenticate(GatewayContext context) {
        String authHeader = context.getHeader(config.getHeaderName());
        if (authHeader == null || !authHeader.startsWith(config.getTokenPrefix())) {
            return Optional.empty();
        }

        String token = authHeader.substring(config.getTokenPrefix().length()).trim();

        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();

            String subject = claims.getSubject();
            Set<String> roles = extractRoles(claims);
            long expiresAt = claims.getExpiration() != null ? claims.getExpiration().getTime() : 0;

            log.debug("JWT authenticated: subject={}, roles={}", subject, roles);
            return Optional.of(new AuthenticationResult(subject, roles, "JWT", expiresAt));

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired for request {}", context.getRequestId());
            return Optional.empty();
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractRoles(Claims claims) {
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?> rolesList) {
            return new HashSet<>((List<String>) rolesList);
        }
        if (rolesObj instanceof String rolesStr) {
            return new HashSet<>(Arrays.asList(rolesStr.split(",")));
        }
        return Set.of();
    }

    /**
     * Generate a JWT token (utility for testing/admin).
     */
    public String generateToken(String subject, Set<String> roles, long expirationMs) {
        return Jwts.builder()
                .subject(subject)
                .issuer(config.getIssuer())
                .claim("roles", new ArrayList<>(roles))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey)
                .compact();
    }
}

