package com.tallersoft.controller;

import com.tallersoft.dto.CajaDiariaResponse;
import com.tallersoft.dto.CobrarOrdenRequest;
import com.tallersoft.dto.CobroResponse;
import com.tallersoft.service.CobrosService;
import com.tallersoft.service.PresupuestoPdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cobros")
@RequiredArgsConstructor
public class CobrosController {

    private final CobrosService cobrosService;
    private final PresupuestoPdfService presupuestoPdfService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public ResponseEntity<CobroResponse> registrarCobro(@Valid @RequestBody CobrarOrdenRequest request) {
        return ResponseEntity.ok(cobrosService.registrarCobro(request));
    }

    @GetMapping("/caja-diaria")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public ResponseEntity<CajaDiariaResponse> getCajaDiaria(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        LocalDate fechaConsulta = fecha != null ? fecha : LocalDate.now();
        return ResponseEntity.ok(cobrosService.getCajaDiaria(fechaConsulta));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public ResponseEntity<CobroResponse> getCobro(@PathVariable Long id) {
        return ResponseEntity.ok(cobrosService.getCobro(id));
    }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public ResponseEntity<CobroResponse> confirmarPagoManual(@PathVariable Long id) {
        return ResponseEntity.ok(cobrosService.confirmarPagoManual(id));
    }

    @GetMapping("/historial")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public ResponseEntity<List<CajaDiariaResponse>> getHistorialCajas(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes) {
        LocalDate now = LocalDate.now();
        int year  = anio != null ? anio : now.getYear();
        int month = mes  != null ? mes  : now.getMonthValue();
        return ResponseEntity.ok(cobrosService.getHistorialCajas(year, month));
    }

    @GetMapping("/ordenes/{ordenId}/presupuesto-pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION', 'TECNICO')")
    public ResponseEntity<byte[]> generarPresupuestoPdf(@PathVariable Long ordenId) {
        byte[] pdf = presupuestoPdfService.generarPresupuesto(ordenId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"presupuesto-orden-" + ordenId + ".pdf\"")
                .body(pdf);
    }
}
