package com.tallersoft.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT Utility for Gateway
 * Validates JWT tokens without signature verification (relies on shared secret)
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Validate JWT token
     * Checks signature and expiration
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            log.debug("Token validated successfully for user: {}", claims.getSubject());
            return true;

        } catch (JwtException | IllegalArgumentException ex) {
            log.error("JWT validation failed: ", ex);
            return false;
        }
    }
}
