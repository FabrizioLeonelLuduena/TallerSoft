package com.tallersoft.controller;

import com.tallersoft.dto.AuthRequest;
import com.tallersoft.dto.AuthResponse;
import com.tallersoft.dto.UsuarioRequest;
import com.tallersoft.dto.UsuarioResponse;
import com.tallersoft.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Authentication Controller
 * Provides endpoints for user login and registration
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Login endpoint
     * POST /auth/login
     *
     * @param request AuthRequest with email and password
     * @return AuthResponse with JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("Login request for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Register endpoint
     * POST /auth/register
     *
     * @param request UsuarioRequest with user details
     * @return UsuarioResponse with created user details
     */
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody UsuarioRequest request) {
        log.info("Registration request for email: {}", request.getEmail());
        UsuarioResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
