package com.tallersoft.controller;

import com.tallersoft.dto.EquipoRequest;
import com.tallersoft.dto.EquipoResponse;
import com.tallersoft.service.EquipoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST Controller for Equipo operations
 * All endpoints require authentication
 */
@Slf4j
@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

    @Autowired
    private EquipoService equipoService;

    /**
     * List all equipment for a specific client
     * GET /api/equipos/cliente/{clienteId}
     *
     * @param clienteId Client ID
     * @return List of EquipoResponse
     */
    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION', 'TECNICO')")
    public ResponseEntity<List<EquipoResponse>> listarPorCliente(@PathVariable Long clienteId) {
        log.info("Listando equipos del cliente: {}", clienteId);
        List<EquipoResponse> equipos = equipoService.listarEquiposDelCliente(clienteId);
        return ResponseEntity.ok(equipos);
    }

    /**
     * Create a new equipment
     * POST /api/equipos
     *
     * @param request EquipoRequest with equipment details
     * @return EquipoResponse
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public ResponseEntity<EquipoResponse> crear(@Valid @RequestBody EquipoRequest request) {
        log.info("Creando nuevo equipo: {}", request.getTipo());
        EquipoResponse equipo = equipoService.crearEquipo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(equipo);
    }

    /**
     * Update an existing equipment
     * PUT /api/equipos/{id}
     *
     * @param id Equipment ID
     * @param request EquipoRequest with updated details
     * @return EquipoResponse
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public ResponseEntity<EquipoResponse> editar(@PathVariable Long id,
                                                  @Valid @RequestBody EquipoRequest request) {
        log.info("Actualizando equipo: {}", id);
        EquipoResponse equipo = equipoService.editarEquipo(id, request);
        return ResponseEntity.ok(equipo);
    }
}
