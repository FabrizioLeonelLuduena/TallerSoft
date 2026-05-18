package com.tallersoft.controller;

import com.tallersoft.dto.ClienteRequest;
import com.tallersoft.dto.ClienteResponse;
import com.tallersoft.service.ClienteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST Controller for Cliente operations
 * All endpoints require authentication
 */
@Slf4j
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    /**
     * List all active clients
     * GET /api/clientes
     *
     * @param nombre Optional name filter
     * @return List of ClienteResponse
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public ResponseEntity<List<ClienteResponse>> listar(
            @RequestParam(required = false) String nombre) {
        log.info("Listando clientes con filtro: {}", nombre);
        List<ClienteResponse> clientes = clienteService.listarClientes(nombre);
        return ResponseEntity.ok(clientes);
    }

    /**
     * Get a specific client by ID
     * GET /api/clientes/{id}
     *
     * @param id Client ID
     * @return ClienteResponse
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public ResponseEntity<ClienteResponse> obtener(@PathVariable Long id) {
        log.info("Obteniendo cliente: {}", id);
        ClienteResponse cliente = clienteService.obtenerCliente(id);
        return ResponseEntity.ok(cliente);
    }

    /**
     * Create a new client
     * POST /api/clientes
     *
     * @param request ClienteRequest with client details
     * @return ClienteResponse
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        log.info("Creando nuevo cliente: {}", request.getNombre());
        ClienteResponse cliente = clienteService.crearCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cliente);
    }

    /**
     * Update an existing client
     * PUT /api/clientes/{id}
     *
     * @param id Client ID
     * @param request ClienteRequest with updated details
     * @return ClienteResponse
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public ResponseEntity<ClienteResponse> editar(@PathVariable Long id,
                                                   @Valid @RequestBody ClienteRequest request) {
        log.info("Actualizando cliente: {}", id);
        ClienteResponse cliente = clienteService.editarCliente(id, request);
        return ResponseEntity.ok(cliente);
    }

    /**
     * Delete (soft delete) a client
     * DELETE /api/clientes/{id}
     *
     * @param id Client ID
     * @return Empty response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("Eliminando cliente: {}", id);
        clienteService.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }
}
