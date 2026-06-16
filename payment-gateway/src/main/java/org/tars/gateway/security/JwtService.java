package org.tars.gateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tars.gateway.config.GatewaySecurityProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final JwtParser parser;
    private final GatewaySecurityProperties.Jwt config;

    public JwtService(GatewaySecurityProperties props) {
        this.config = props.getJwt();
        this.signingKey = Keys.hmacShaKeyFor(config.getSecret().getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser().verifyWith(signingKey).build();
    }

    public Optional<JwtClaims> validate(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            return Optional.of(new JwtClaims(claims.getSubject(), extractRoles(claims)));
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired");
        } catch (JwtException e) {
            log.warn("JWT invalid: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public String generateToken(String subject, Set<String> roles, long expirationMs) {
        return Jwts.builder()
                .subject(subject).issuer(config.getIssuer())
                .claim("roles", new ArrayList<>(roles))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey).compact();
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractRoles(Claims c) {
        Object r = c.get("roles");
        if (r instanceof List<?> list) return new HashSet<>((List<String>) list);
        if (r instanceof String s) return new HashSet<>(Arrays.asList(s.split(",")));
        return Set.of();
    }

    public record JwtClaims(String subject, Set<String> roles) {}
}
