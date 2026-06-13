package com.tallersoft.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class MercadoPagoWebhookValidator {

    @Value("${mercadopago.webhook-secret}")
    private String webhookSecret;

    @Value("${mercadopago.sandbox}")
    private boolean sandbox;

    @PostConstruct
    private void validateWebhookSecret() {
        if (!sandbox && (webhookSecret == null || webhookSecret.isBlank())) {
            throw new IllegalStateException(
                "mercadopago.webhook-secret debe estar configurado en producción. " +
                "Seteá la variable de entorno MP_WEBHOOK_SECRET."
            );
        }
    }

    /**
     * Valida la firma del header x-signature de MercadoPago.
     * Formato del header: "ts=<timestamp>,v1=<hash>"
     * Mensaje a hashear: "id:<paymentId>;request-id:<requestId>;ts:<ts>;"
     */
    public void validar(String signature, String requestId, String paymentId) {
        if (signature == null || signature.isBlank()) {
            throw new SecurityException("Header x-signature ausente o vacío");
        }

        String ts = extraerValor(signature, "ts");
        String v1 = extraerValor(signature, "v1");

        if (ts == null || v1 == null) {
            throw new SecurityException("Formato de x-signature inválido");
        }

        String mensaje = "id:" + paymentId + ";request-id:" + requestId + ";ts:" + ts + ";";
        String hashCalculado = calcularHmac(mensaje, webhookSecret);

        if (!hashCalculado.equals(v1)) {
            log.warn("Firma de webhook MP inválida. Esperado: {}, Recibido: {}", hashCalculado, v1);
            throw new SecurityException("Firma de webhook de MercadoPago inválida");
        }

        log.debug("Firma de webhook MP válida para paymentId={}", paymentId);
    }

    private String extraerValor(String signature, String clave) {
        for (String parte : signature.split(",")) {
            String[] kv = parte.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals(clave)) {
                return kv[1];
            }
        }
        return null;
    }

    private String calcularHmac(String mensaje, String secreto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(mensaje.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculando HMAC-SHA256", e);
        }
    }
}
