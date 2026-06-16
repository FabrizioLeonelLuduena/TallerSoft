package com.tallersoft.controller;

import com.tallersoft.model.Usuario;
import com.tallersoft.service.AlertaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
@Slf4j
public class AlertaController {

    private final AlertaService alertaService;

    /**
     * Retorna la lista de alertaKeys que el usuario autenticado ya leyó.
     */
    @GetMapping("/leidas")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO', 'RECEPCION')")
    public ResponseEntity<List<String>> getAlertasLeidas(Authentication auth) {
        Long usuarioId = ((Usuario) auth.getPrincipal()).getId();
        return ResponseEntity.ok(alertaService.obtenerAlertasLeidas(usuarioId));
    }

    /**
     * Marca una alerta como leída para el usuario autenticado.
     * Idempotente: si ya estaba leída, devuelve 200 igualmente.
     */
    @PostMapping("/{alertaKey}/leer")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO', 'RECEPCION')")
    public ResponseEntity<Map<String, Object>> marcarLeida(
            @PathVariable String alertaKey,
            Authentication auth) {
        Long usuarioId = ((Usuario) auth.getPrincipal()).getId();
        alertaService.marcarLeida(usuarioId, alertaKey);
        log.info("Alerta '{}' marcada como leída por usuario {}", alertaKey, usuarioId);
        return ResponseEntity.ok(Map.of("ok", true, "alertaKey", alertaKey));
    }
}
