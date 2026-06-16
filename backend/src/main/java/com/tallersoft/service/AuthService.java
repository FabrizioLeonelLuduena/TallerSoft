package com.tallersoft.service;

import com.tallersoft.dto.AuthRequest;
import com.tallersoft.dto.AuthResponse;
import com.tallersoft.dto.UsuarioRequest;
import com.tallersoft.dto.UsuarioResponse;
import com.tallersoft.exception.InvalidCredentialsException;
import com.tallersoft.model.Rol;
import com.tallersoft.model.Usuario;
import com.tallersoft.repository.UsuarioRepository;
import com.tallersoft.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service
 * Handles user login and registration
 */
@Slf4j
@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Login user with email and password
     *
     * @param request Login request with email and password
     * @return AuthResponse with JWT token
     * @throws InvalidCredentialsException if credentials are invalid
     */
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Intento de login con email no registrado: {}", request.getEmail());
                    return new InvalidCredentialsException("Email o contraseña incorrectos");
                });

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            log.warn("Intento de login con contraseña incorrecta para usuario: {}", 
                    request.getEmail());
            throw new InvalidCredentialsException("Email o contraseña incorrectos");
        }

        if (!usuario.isActivo()) {
            log.warn("Intento de login con usuario inactivo: {}", request.getEmail());
            throw new InvalidCredentialsException("Usuario inactivo");
        }

        String token = jwtUtil.generateToken(usuario.getId(), usuario.getEmail(), 
                usuario.getRol().getValue());
        log.info("Login exitoso para usuario: {}", request.getEmail());

        return new AuthResponse(token, usuario.getId(), usuario.getEmail(), 
                usuario.getRol().getValue());
    }

    /**
     * Register a new user
     *
     * @param request Registration request with user details
     * @return UsuarioResponse with created user details
     * @throws IllegalArgumentException if email already exists
     */
    @Transactional
    public UsuarioResponse register(UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            log.warn("Intento de registro con email ya existente: {}", request.getEmail());
            throw new IllegalArgumentException("El email ya está registrado");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(Rol.fromString(request.getRol()))
                .activo(true)
                .build();

        Usuario savedUsuario = usuarioRepository.save(usuario);
        log.info("Nuevo usuario registrado: {}", request.getEmail());

        return new UsuarioResponse(savedUsuario.getId(), savedUsuario.getNombre(),
                savedUsuario.getEmail(), savedUsuario.getTelefono(),
                savedUsuario.getRol().getValue(),
                savedUsuario.isActivo(), savedUsuario.getCreatedAt(), savedUsuario.getAvatarImage());
    }
}
