package com.tallersoft.controller;

import com.tallersoft.dto.RepuestoRequest;
import com.tallersoft.dto.RepuestoResponse;
import com.tallersoft.service.RepuestoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repuestos")
@RequiredArgsConstructor
@Slf4j
public class RepuestoController {
    
    private final RepuestoService repuestoService;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO', 'RECEPCION')")
    public ResponseEntity<List<RepuestoResponse>> listarRepuestos(
            @RequestParam(required = false) Boolean critico) {
        log.info("Listando repuestos, critico={}", critico);
        
        List<RepuestoResponse> repuestos;
        if (critico != null && critico) {
            repuestos = repuestoService.listarRepuestosCriticos();
        } else {
            repuestos = repuestoService.listarRepuestos();
        }
        
        return ResponseEntity.ok(repuestos);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO', 'RECEPCION')")
    public ResponseEntity<RepuestoResponse> obtenerRepuesto(@PathVariable Long id) {
        log.info("Obteniendo repuesto {}", id);
        RepuestoResponse repuesto = repuestoService.obtenerRepuesto(id);
        return ResponseEntity.ok(repuesto);
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RepuestoResponse> crearRepuesto(@Valid @RequestBody RepuestoRequest request) {
        log.info("Creando nuevo repuesto");
        RepuestoResponse repuesto = repuestoService.crearRepuesto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(repuesto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RepuestoResponse> editarRepuesto(
            @PathVariable Long id,
            @Valid @RequestBody RepuestoRequest request) {
        log.info("Editando repuesto {}", id);
        RepuestoResponse repuesto = repuestoService.editarRepuesto(id, request);
        return ResponseEntity.ok(repuesto);
    }
}
