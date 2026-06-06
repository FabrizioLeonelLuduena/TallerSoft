package com.tallersoft.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallersoft.dto.AuthRequest;
import com.tallersoft.model.Rol;
import com.tallersoft.model.Usuario;
import com.tallersoft.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController endpoints.
 * Uses @SpringBootTest with test profile (H2 in-memory database).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
        usuarioRepository.save(Usuario.builder()
                .nombre("Admin Test")
                .email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .rol(Rol.ADMIN)
                .activo(true)
                .build());
    }

    @Test
    @DisplayName("POST /auth/login con credenciales válidas → 200 OK con JWT")
    void login_credencialesValidas_deberiaRetornar200ConToken() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("admin@test.com");
        request.setPassword("admin123");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", is("admin@test.com")))
                .andExpect(jsonPath("$.rol", is("ADMIN")));
    }

    @Test
    @DisplayName("POST /auth/login con contraseña incorrecta → 401 Unauthorized")
    void login_contrasenaIncorrecta_deberiaRetornar401() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("admin@test.com");
        request.setPassword("wrongPassword");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login con email inexistente → 401 Unauthorized")
    void login_emailInexistente_deberiaRetornar401() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("noexiste@test.com");
        request.setPassword("cualquiera");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login con body vacío → 400 Bad Request")
    void login_bodyVacio_deberiaRetornar400() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
