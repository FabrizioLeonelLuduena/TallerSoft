package com.tallersoft.controller;

import com.tallersoft.dto.UsuarioRequest;
import com.tallersoft.dto.UsuarioResponse;
import com.tallersoft.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/api/usuarios")
public class UsuariosController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * List all users
     * GET /api/usuarios
     * 
     * @param rol Optional role filter
     * @return List of UsuarioResponse
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listar(
            @RequestParam(required = false) String rol) {
        log.info("Listando usuarios con filtro rol: {}", rol);
        List<UsuarioResponse> usuarios;
        
        if (rol != null && !rol.isBlank()) {
            usuarios = usuarioService.listarUsuariosPorRol(rol);
        } else {
            usuarios = usuarioService.listarUsuarios();
        }
        
        return ResponseEntity.ok(usuarios);
    }

    /**
     * Get a specific user by ID
     * GET /api/usuarios/{id}
     *
     * @param id User ID
     * @return UsuarioResponse
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Long id) {
        log.info("Obteniendo usuario: {}", id);
        UsuarioResponse usuario = usuarioService.obtenerUsuario(id);
        return ResponseEntity.ok(usuario);
    }

    /**
     * Create a new user
     * POST /api/usuarios
     *
     * @param request UsuarioRequest with user details
     * @return UsuarioResponse
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        log.info("Creando nuevo usuario: {}", request.getEmail());
        UsuarioResponse usuario = usuarioService.crearUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    /**
     * Update an existing user
     * PUT /api/usuarios/{id}
     *
     * @param id User ID
     * @param request UsuarioRequest with updated details
     * @return UsuarioResponse
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> editar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioRequest request) {
        log.info("Actualizando usuario: {}", id);
        UsuarioResponse usuario = usuarioService.editarUsuario(id, request);
        return ResponseEntity.ok(usuario);
    }

    /**
     * Deactivate a user (soft delete)
     * DELETE /api/usuarios/{id}
     *
     * @param id User ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        log.info("Desactivando usuario: {}", id);
        usuarioService.desactivarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activate a user
     * PATCH /api/usuarios/{id}/activar
     *
     * @param id User ID
     * @return UsuarioResponse
     */
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> activar(@PathVariable Long id) {
        log.info("Activando usuario: {}", id);
        usuarioService.activarUsuario(id);
        UsuarioResponse usuario = usuarioService.obtenerUsuario(id);
        return ResponseEntity.ok(usuario);
    }

    /**
     * DEBUG endpoint - Check current authentication (REQUIRES AUTH)
     * GET /api/usuarios/debug/me
     */
    @GetMapping("/debug/me")
    public ResponseEntity<Map<String, Object>> debugMe() {
        Map<String, Object> debug = new HashMap<>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        debug.put("authenticated", auth != null && auth.isAuthenticated());
        if (auth != null) {
            debug.put("principal", auth.getPrincipal().toString());
            debug.put("authorities", auth.getAuthorities().stream()
                    .map(Object::toString)
                    .toList());
            debug.put("name", auth.getName());
            debug.put("details", auth.getDetails().toString());
        }
        
        return ResponseEntity.ok(debug);
    }

    /**
     * DEBUG endpoint - Generate BCrypt hash for password
     * POST /api/usuarios/debug/hash-password
     * Body: { "password": "yourpassword" }
     */
    @PostMapping("/debug/hash-password")
    public ResponseEntity<Map<String, Object>> generatePasswordHash(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "password is required"));
        }

        String hash = passwordEncoder.encode(password);
        Map<String, Object> response = new HashMap<>();
        response.put("password", password);
        response.put("hash", hash);
        response.put("message", "Use this hash in the database UPDATE statement");

        log.info("Generated hash for password: {}", password);
        return ResponseEntity.ok(response);
    }
}
