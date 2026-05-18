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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthService
 * Tests login and registration flows
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private Usuario testUsuario;

    @BeforeEach
    void setUp() {
        testUsuario = Usuario.builder()
                .id(1L)
                .nombre("Test User")
                .email("test@example.com")
                .password("hashedPassword123")
                .rol(Rol.ADMIN)
                .activo(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testLogin_ValidCredentials() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(usuarioRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUsuario));
        when(passwordEncoder.matches("password123", "hashedPassword123"))
                .thenReturn(true);
        when(jwtUtil.generateToken(1L, "test@example.com", "ADMIN"))
                .thenReturn("token123");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("token123", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("ADMIN", response.getRol());
    }

    @Test
    void testLogin_InvalidEmail() {
        AuthRequest request = new AuthRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        when(usuarioRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void testLogin_WrongPassword() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongPassword");

        when(usuarioRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUsuario));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword123"))
                .thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void testLogin_InactiveUsuario() {
        testUsuario.setActivo(false);
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(usuarioRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUsuario));
        when(passwordEncoder.matches("password123", "hashedPassword123"))
                .thenReturn(true);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void testRegister_ValidRequest() {
        UsuarioRequest request = new UsuarioRequest();
        request.setNombre("New User");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setRol("TECNICO");

        when(usuarioRepository.existsByEmail("newuser@example.com"))
                .thenReturn(false);
        when(passwordEncoder.encode("password123"))
                .thenReturn("hashedPassword123");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenReturn(Usuario.builder()
                        .id(2L)
                        .nombre("New User")
                        .email("newuser@example.com")
                        .password("hashedPassword123")
                        .rol(Rol.TECNICO)
                        .activo(true)
                        .createdAt(LocalDateTime.now())
                        .build());

        UsuarioResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("newuser@example.com", response.getEmail());
        assertEquals("TECNICO", response.getRol());
        assertTrue(response.isActivo());
    }

    @Test
    void testRegister_EmailAlreadyExists() {
        UsuarioRequest request = new UsuarioRequest();
        request.setNombre("New User");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setRol("TECNICO");

        when(usuarioRepository.existsByEmail("test@example.com"))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }
}
