package com.tallersoft.service;

import com.tallersoft.dto.UsuarioRequest;
import com.tallersoft.dto.UsuarioResponse;
import com.tallersoft.dto.UsuarioUpdateRequest;
import com.tallersoft.exception.EntityNotFoundException;
import com.tallersoft.mapper.UsuarioMapper;
import com.tallersoft.model.Rol;
import com.tallersoft.model.Usuario;
import com.tallersoft.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic service for Usuario (User) entity
 * Handles user management operations (CRUD)
 */
@Slf4j
@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioMapper usuarioMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Create a new user (Only for ADMIN)
     *
     * @param request UsuarioRequest with user details
     * @return UsuarioResponse with created user
     */
    @Transactional
    public UsuarioResponse crearUsuario(UsuarioRequest request) {
        // Check if email already exists
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado: " + request.getEmail());
        }

        Usuario usuario = usuarioMapper.toEntity(request);
        usuario.setActivo(true);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // Parse rol string to Rol enum
        try {
            usuario.setRol(Rol.fromString(request.getRol()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol inválido: " + request.getRol());
        }

        Usuario saved = usuarioRepository.save(usuario);
        log.info("Usuario creado: {}", saved.getNombre());
        return usuarioMapper.toResponse(saved);
    }

    /**
     * Get user by ID
     *
     * @param id User ID
     * @return UsuarioResponse
     * @throws EntityNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + id));
        return usuarioMapper.toResponse(usuario);
    }

    /**
     * List all active users, optionally filtered by role
     *
     * @return List of UsuarioResponse
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        return usuarioMapper.toResponseList(usuarios);
    }

    /**
     * List users by specific role
     *
     * @param rol Role filter
     * @return List of UsuarioResponse
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuariosPorRol(String rol) {
        try {
            Rol rolEnum = Rol.fromString(rol);
            List<Usuario> usuarios = usuarioRepository.findByRol(rolEnum);
            return usuarioMapper.toResponseList(usuarios);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol inválido: " + rol);
        }
    }

    /**
     * Update an existing user
     *
     * @param id User ID
     * @param request UsuarioRequest with updated details
     * @return UsuarioResponse
     * @throws EntityNotFoundException if user not found
     */
    @Transactional
    public UsuarioResponse editarUsuario(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + id));

        // Check if email is being changed and if new email already exists
        if (!usuario.getEmail().equals(request.getEmail()) && 
            usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado: " + request.getEmail());
        }

        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        
        // Only update password if provided
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        // Update role if provided
        if (request.getRol() != null && !request.getRol().isBlank()) {
            try {
                usuario.setRol(Rol.fromString(request.getRol()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Rol inválido: " + request.getRol());
            }
        }

        Usuario updated = usuarioRepository.save(usuario);
        log.info("Usuario actualizado: {}", updated.getNombre());
        return usuarioMapper.toResponse(updated);
    }

    /**
     * Delete a user (hard delete - removes from database)
     *
     * @param id User ID
     * @throws EntityNotFoundException if user not found
     */
    @Transactional
    public void desactivarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + id));
        
        usuarioRepository.delete(usuario);
        log.info("Usuario eliminado: {}", usuario.getNombre());
    }

    /**
     * Activate a user (sets activo=true)
     *
     * @param id User ID
     * @throws EntityNotFoundException if user not found
     */
    @Transactional
    public void activarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + id));
        
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
        log.info("Usuario activado: {}", usuario.getNombre());
    }

    /**
     * Get user by email (used for authentication)
     *
     * @param email User email
     * @return UsuarioResponse
     * @throws EntityNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuarioPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con email: " + email));
        return usuarioMapper.toResponse(usuario);
    }
}
