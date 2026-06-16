package org.tars.gateway.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tars.gateway.config.GatewayProperties;
import org.tars.gateway.context.GatewayContext;
import org.tars.gateway.security.AuthenticationResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JWT authentication provider.
 * Validates Bearer tokens and extracts subject/roles.
 */
@Slf4j
@Component
public class JwtAuthProvider {

    private final GatewayProperties.Jwt config;
    private final SecretKey signingKey;
    private final JwtParser parser;

    public JwtAuthProvider(GatewayProperties properties) {
        this.config = properties.getSecurity().getJwt();
        this.signingKey = Keys.hmacShaKeyFor(config.getSecret().getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser().verifyWith(signingKey).build();
    }

    public Optional<AuthenticationResult> authenticate(GatewayContext context) {
        if (!config.isEnabled()) return Optional.empty();

        String header = context.getHeader(config.getHeaderName());
        if (header == null || !header.startsWith(config.getTokenPrefix())) {
            return Optional.empty();
        }

        String token = header.substring(config.getTokenPrefix().length()).trim();
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            String subject = claims.getSubject();
            Set<String> roles = extractRoles(claims);
            return Optional.of(new AuthenticationResult(subject, roles, "JWT"));
        } catch (ExpiredJwtException e) {
            log.warn("[{}] JWT expired", context.getRequestId());
        } catch (JwtException e) {
            log.warn("[{}] JWT invalid: {}", context.getRequestId(), e.getMessage());
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractRoles(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list) return new HashSet<>((List<String>) list);
        if (roles instanceof String s) return new HashSet<>(Arrays.asList(s.split(",")));
        return Set.of();
    }

    /** Generate token — useful for testing and admin endpoints. */
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

