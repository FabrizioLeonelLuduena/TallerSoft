package com.tallersoft.service;

import com.tallersoft.dto.UsuarioRequest;
import com.tallersoft.dto.UsuarioResponse;
import com.tallersoft.dto.UsuarioUpdateRequest;
import com.tallersoft.exception.EntityNotFoundException;
import com.tallersoft.mapper.UsuarioMapper;
import com.tallersoft.model.Rol;
import com.tallersoft.model.Usuario;
import com.tallersoft.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioMapper usuarioMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UsuarioService usuarioService;

    private Usuario usuario;
    private UsuarioResponse usuarioResponse;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nombre("Carlos Gómez")
                .email("carlos@tallersoft.com")
                .password("$2a$10$hashedPassword")
                .rol(Rol.TECNICO)
                .activo(true)
                .createdAt(LocalDateTime.now())
                .build();

        usuarioResponse = new UsuarioResponse(1L, "Carlos Gómez", "carlos@tallersoft.com",
                "TECNICO", true, LocalDateTime.now());
    }

    // ─── crearUsuario ────────────────────────────────────────────────────────

    @Test
    @DisplayName("crearUsuario: email nuevo → usuario guardado con password hasheado")
    void crearUsuario_deberiaCrearConPasswordHasheado() {
        UsuarioRequest request = new UsuarioRequest();
        request.setNombre("Carlos Gómez");
        request.setEmail("carlos@tallersoft.com");
        request.setPassword("password123");
        request.setRol("TECNICO");

        when(usuarioRepository.findByEmail("carlos@tallersoft.com")).thenReturn(Optional.empty());
        when(usuarioMapper.toEntity(request)).thenReturn(usuario);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.toResponse(usuario)).thenReturn(usuarioResponse);

        UsuarioResponse resultado = usuarioService.crearUsuario(request);

        assertNotNull(resultado);
        assertEquals("carlos@tallersoft.com", resultado.getEmail());
        verify(passwordEncoder, times(1)).encode("password123");
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("crearUsuario: email duplicado → lanza IllegalArgumentException")
    void crearUsuario_deberiaLanzarExcepcionCuandoEmailDuplicado() {
        UsuarioRequest request = new UsuarioRequest();
        request.setEmail("carlos@tallersoft.com");
        request.setRol("TECNICO");

        when(usuarioRepository.findByEmail("carlos@tallersoft.com"))
                .thenReturn(Optional.of(usuario));

        assertThrows(IllegalArgumentException.class,
                () -> usuarioService.crearUsuario(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearUsuario: rol inválido → lanza IllegalArgumentException")
    void crearUsuario_deberiaLanzarExcepcionCuandoRolInvalido() {
        UsuarioRequest request = new UsuarioRequest();
        request.setEmail("nuevo@test.com");
        request.setPassword("pass");
        request.setRol("INVALIDO");

        when(usuarioRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.empty());
        when(usuarioMapper.toEntity(request)).thenReturn(usuario);

        assertThrows(IllegalArgumentException.class,
                () -> usuarioService.crearUsuario(request));
    }

    // ─── listarUsuarios ──────────────────────────────────────────────────────

    @Test
    @DisplayName("listarUsuarios: retorna todos los usuarios correctamente")
    void listarUsuarios_deberiaRetornarTodos() {
        List<Usuario> usuarios = List.of(usuario);
        List<UsuarioResponse> responses = List.of(usuarioResponse);

        when(usuarioRepository.findAll()).thenReturn(usuarios);
        when(usuarioMapper.toResponseList(usuarios)).thenReturn(responses);

        List<UsuarioResponse> resultado = usuarioService.listarUsuarios();

        assertEquals(1, resultado.size());
        verify(usuarioRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("listarUsuarios: repositorio vacío → retorna lista vacía")
    void listarUsuarios_deberiaRetornarListaVacia() {
        when(usuarioRepository.findAll()).thenReturn(List.of());
        when(usuarioMapper.toResponseList(List.of())).thenReturn(List.of());

        List<UsuarioResponse> resultado = usuarioService.listarUsuarios();

        assertTrue(resultado.isEmpty());
    }

    // ─── obtenerUsuario ──────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerUsuario: id existente → retorna el usuario")
    void obtenerUsuario_deberiaRetornarUsuarioCuandoExiste() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioMapper.toResponse(usuario)).thenReturn(usuarioResponse);

        UsuarioResponse resultado = usuarioService.obtenerUsuario(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
    }

    @Test
    @DisplayName("obtenerUsuario: id inexistente → lanza EntityNotFoundException")
    void obtenerUsuario_deberiaLanzarExcepcionCuandoNoExiste() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> usuarioService.obtenerUsuario(999L));
    }

    // ─── editarUsuario ───────────────────────────────────────────────────────

    @Test
    @DisplayName("editarUsuario: datos válidos → actualiza y retorna")
    void editarUsuario_deberiaActualizarCorrectamente() {
        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setNombre("Carlos Actualizado");
        request.setEmail("carlos@tallersoft.com");
        request.setRol("ADMIN");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.toResponse(usuario)).thenReturn(usuarioResponse);

        UsuarioResponse resultado = usuarioService.editarUsuario(1L, request);

        assertNotNull(resultado);
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("editarUsuario: id inexistente → lanza EntityNotFoundException")
    void editarUsuario_deberiaLanzarExcepcionCuandoNoExiste() {
        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setEmail("any@test.com");

        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> usuarioService.editarUsuario(999L, request));
    }

    // ─── desactivarUsuario ───────────────────────────────────────────────────

    @Test
    @DisplayName("desactivarUsuario: id existente → elimina usuario")
    void desactivarUsuario_deberiaEliminarCuandoExiste() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        usuarioService.desactivarUsuario(1L);

        verify(usuarioRepository, times(1)).delete(usuario);
    }

    @Test
    @DisplayName("desactivarUsuario: id inexistente → lanza EntityNotFoundException")
    void desactivarUsuario_deberiaLanzarExcepcionCuandoNoExiste() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> usuarioService.desactivarUsuario(999L));
    }
}
