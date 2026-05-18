package com.tallersoft.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Utility class for token generation, validation, and claim extraction.
 * Uses HS256 algorithm with a secret key from environment variables.
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:clave_secreta_minimo_256_bits_para_produccion_cambiar_esto}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    /**
     * Generate a JWT token with user information
     *
     * @param userId User ID
     * @param email User email
     * @param rol User role
     * @return JWT token
     */
    public String generateToken(Long userId, String email, String rol) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("rol", rol);
        return createToken(claims, email);
    }

    /**
     * Validate a JWT token
     *
     * @param token JWT token
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract user ID from token
     *
     * @param token JWT token
     * @return User ID
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        }
        return ((Number) userIdObj).longValue();
    }

    /**
     * Extract email from token
     *
     * @param token JWT token
     * @return Email
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extract role from token
     *
     * @param token JWT token
     * @return Role
     */
    public String extractRole(String token) {
        return (String) extractAllClaims(token).get("rol");
    }

    /**
     * Extract all claims from token
     *
     * @param token JWT token
     * @return Claims object
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Create JWT token with claims
     *
     * @param claims Token claims
     * @param subject Token subject (email)
     * @return JWT token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Get signing key for JWT operations
     *
     * @return SecretKey
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}
