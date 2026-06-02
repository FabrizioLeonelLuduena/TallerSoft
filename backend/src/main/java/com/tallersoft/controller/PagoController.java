package com.tallersoft.controller;

import com.tallersoft.security.MercadoPagoWebhookValidator;
import com.tallersoft.service.CobrosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@Slf4j
public class PagoController {

    private final CobrosService cobrosService;
    private final MercadoPagoWebhookValidator webhookValidator;

    /**
     * Endpoint público — sin JWT. MercadoPago lo llama al confirmar o rechazar un pago.
     * Siempre retorna 200 OK para evitar reintentos de MP.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> recibirWebhook(
            @RequestParam String type,
            @RequestParam("data.id") String paymentId,
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-request-id", required = false) String requestId) {

        log.info("Webhook MercadoPago recibido: type={}, paymentId={}", type, paymentId);

        try {
            if (signature != null && requestId != null) {
                webhookValidator.validar(signature, requestId, paymentId);
            } else {
                log.warn("Webhook sin headers de firma — aceptado solo en entorno de pruebas");
            }

            if ("payment".equals(type)) {
                cobrosService.procesarPagoAprobadoMercadoPago(paymentId);
            }

        } catch (SecurityException e) {
            log.warn("Firma de webhook inválida: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error procesando webhook MP paymentId={}: {}", paymentId, e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
