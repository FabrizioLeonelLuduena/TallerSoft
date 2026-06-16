package com.tallersoft.controller;

import com.tallersoft.dto.*;
import com.tallersoft.model.EstadoOrden;
import com.tallersoft.model.Usuario;
import com.tallersoft.service.OrdenTrabajoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ordenes")
@RequiredArgsConstructor
@Slf4j
public class OrdenTrabajoController {
    
    private final OrdenTrabajoService ordenTrabajoService;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO', 'RECEPCION')")
    public ResponseEntity<List<OrdenTrabajoResponse>> listarOrdenes(
            @RequestParam(required = false) EstadoOrden estado,
            @RequestParam(required = false) Long tecnicoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication auth) {
        boolean isTecnico = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TECNICO"));
        if (isTecnico) {
            tecnicoId = ((Usuario) auth.getPrincipal()).getId();
            desde = null;
            hasta = null;
        }
        log.info("Listando órdenes con filtros: estado={}, tecnicoId={}, desde={}, hasta={}",
                estado, tecnicoId, desde, hasta);
        List<OrdenTrabajoResponse> ordenes = ordenTrabajoService.listarOrdenes(estado, tecnicoId, desde, hasta);
        return ResponseEntity.ok(ordenes);
    }

    @GetMapping("/activas")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO', 'RECEPCION')")
    public ResponseEntity<List<OrdenTrabajoResponse>> listarOrdenesActivas(Authentication auth) {
        boolean isTecnico = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TECNICO"));
        List<OrdenTrabajoResponse> ordenes = isTecnico
                ? ordenTrabajoService.listarOrdenesActivas(((Usuario) auth.getPrincipal()).getId())
                : ordenTrabajoService.listarOrdenesActivas();
        log.info("Listando órdenes activas (tecnico={})", isTecnico);
        return ResponseEntity.ok(ordenes);
    }
    
    @GetMapping("/mis-ordenes")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<List<OrdenTrabajoResponse>> misPropias(
            @RequestParam(required = false) EstadoOrden estado,
            Authentication auth) {
        Usuario usuario = (Usuario) auth.getPrincipal();
        log.info("Listando órdenes del técnico {} con estado: {}", usuario.getId(), estado);
        List<OrdenTrabajoResponse> ordenes = ordenTrabajoService.listarOrdenes(estado, usuario.getId(), null, null);
        return ResponseEntity.ok(ordenes);
    }
    
    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO', 'RECEPCION')")
    public ResponseEntity<List<OrdenTrabajoResponse>> listarOrdenesPorCliente(@PathVariable Long clienteId) {
        log.info("Listando órdenes del cliente {}", clienteId);
        return ResponseEntity.ok(ordenTrabajoService.listarOrdenesPorCliente(clienteId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO', 'RECEPCION')")
    public ResponseEntity<OrdenTrabajoResponse> obtenerOrden(@PathVariable Long id) {
        log.info("Obteniendo orden {}", id);
        OrdenTrabajoResponse orden = ordenTrabajoService.obtenerOrden(id);
        return ResponseEntity.ok(orden);
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public ResponseEntity<OrdenTrabajoResponse> crearOrden(@Valid @RequestBody OrdenTrabajoRequest request) {
        log.info("Creando nueva orden");
        OrdenTrabajoResponse orden = ordenTrabajoService.crearOrden(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(orden);
    }
    
    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO', 'RECEPCION')")
    public ResponseEntity<OrdenTrabajoResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoRequest request,
            Authentication authentication) {
        log.info("Cambiando estado de orden {} a {}", id, request.getNuevoEstado());
        OrdenTrabajoResponse orden = ordenTrabajoService.cambiarEstado(id, request.getNuevoEstado(), authentication);
        return ResponseEntity.ok(orden);
    }
    
    @PutMapping("/{id}/diagnostico")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO')")
    public ResponseEntity<OrdenTrabajoResponse> agregarDiagnostico(
            @PathVariable Long id,
            @Valid @RequestBody DiagnosticoRequest request) {
        log.info("Agregando diagnóstico a orden {}", id);
        OrdenTrabajoResponse orden = ordenTrabajoService.agregarDiagnostico(id, request.getDiagnostico());
        return ResponseEntity.ok(orden);
    }
    
    @PostMapping("/{id}/repuestos")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO')")
    public ResponseEntity<OrdenTrabajoResponse> agregarRepuesto(
            @PathVariable Long id,
            @Valid @RequestBody AgregarRepuestoRequest request) {
        log.info("Agregando repuesto a orden {}", id);
        OrdenTrabajoResponse orden = ordenTrabajoService.agregarRepuesto(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(orden);
    }
    
    @GetMapping("/{id}/repuestos")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO', 'RECEPCION')")
    public ResponseEntity<OrdenTrabajoResponse> obtenerRepuestosDeOrden(@PathVariable Long id) {
        log.info("Obteniendo repuestos de orden {}", id);
        OrdenTrabajoResponse orden = ordenTrabajoService.obtenerOrden(id);
        return ResponseEntity.ok(orden);
    }

    @DeleteMapping("/{id}/repuestos/{ordenRepuestoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO')")
    public ResponseEntity<OrdenTrabajoResponse> eliminarRepuesto(
            @PathVariable Long id,
            @PathVariable Long ordenRepuestoId) {
        log.info("Eliminando repuesto {} de orden {}", ordenRepuestoId, id);
        OrdenTrabajoResponse orden = ordenTrabajoService.eliminarRepuesto(id, ordenRepuestoId);
        return ResponseEntity.ok(orden);
    }
}
