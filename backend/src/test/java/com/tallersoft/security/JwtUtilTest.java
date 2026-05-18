package com.tallersoft.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtUtil
 * Tests token generation, validation, and claim extraction
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String testSecret = "mySecretKeyForTestingJwtTokenGenerationAndValidation123456789";
    private long testExpiration = 86400000; // 1 day

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", testExpiration);
    }

    @Test
    void testGenerateToken() {
        String token = jwtUtil.generateToken(1L, "test@example.com", "ADMIN");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testValidateToken_ValidToken() {
        String token = jwtUtil.generateToken(1L, "test@example.com", "ADMIN");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateToken_InvalidToken() {
        String invalidToken = "invalid.token.here";
        assertFalse(jwtUtil.validateToken(invalidToken));
    }

    @Test
    void testValidateToken_ExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", -1000L); // Set expiration to past
        String expiredToken = jwtUtil.generateToken(1L, "test@example.com", "ADMIN");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", testExpiration); // Reset
        
        assertFalse(jwtUtil.validateToken(expiredToken));
    }

    @Test
    void testExtractUserId() {
        String token = jwtUtil.generateToken(123L, "test@example.com", "ADMIN");
        Long userId = jwtUtil.extractUserId(token);
        assertEquals(123L, userId);
    }

    @Test
    void testExtractEmail() {
        String token = jwtUtil.generateToken(1L, "test@example.com", "ADMIN");
        String email = jwtUtil.extractEmail(token);
        assertEquals("test@example.com", email);
    }

    @Test
    void testExtractRole() {
        String token = jwtUtil.generateToken(1L, "test@example.com", "TECNICO");
        String rol = jwtUtil.extractRole(token);
        assertEquals("TECNICO", rol);
    }

    @Test
    void testTokenWithDifferentRoles() {
        String adminToken = jwtUtil.generateToken(1L, "admin@example.com", "ADMIN");
        String tecnicoToken = jwtUtil.generateToken(2L, "tecnico@example.com", "TECNICO");
        String recepcionToken = jwtUtil.generateToken(3L, "recepcion@example.com", "RECEPCION");

        assertEquals("ADMIN", jwtUtil.extractRole(adminToken));
        assertEquals("TECNICO", jwtUtil.extractRole(tecnicoToken));
        assertEquals("RECEPCION", jwtUtil.extractRole(recepcionToken));
    }
}
