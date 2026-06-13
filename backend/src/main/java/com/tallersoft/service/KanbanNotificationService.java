package com.tallersoft.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KanbanNotificationService {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public void notificarCambioOrden(Long ordenId, String nuevoEstado) {
        Map<String, Object> payload = Map.of(
                "ordenId", ordenId,
                "nuevoEstado", nuevoEstado
        );
        log.debug("Publicando cambio de estado kanban: orden={} estado={}", ordenId, nuevoEstado);
        simpMessagingTemplate.convertAndSend("/topic/kanban", payload);
    }
}
