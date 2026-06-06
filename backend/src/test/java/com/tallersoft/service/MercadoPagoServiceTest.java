package com.tallersoft.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MercadoPagoService.
 *
 * La generación de preferencias y el envío de QR POS requieren llamadas reales
 * al SDK de MercadoPago (que no se puede inyectar por @Mock en la implementación
 * actual). Estos tests cubren la validación de firma HMAC del webhook mediante
 * MercadoPagoWebhookValidator, y verifican el comportamiento de error cuando
 * el accessToken está vacío.
 */
@ExtendWith(MockitoExtension.class)
class MercadoPagoServiceTest {

    // ─── Webhook signature validation ────────────────────────────────────────

    @Test
    @DisplayName("validarFirmaWebhook: firma válida → no lanza excepción")
    void validarFirmaWebhook_firmaValida_noDeberiaLanzarExcepcion() {
        com.tallersoft.security.MercadoPagoWebhookValidator validator =
                new com.tallersoft.security.MercadoPagoWebhookValidator();
        ReflectionTestUtils.setField(validator, "webhookSecret", "test_secret_key");

        // Construir firma en el formato que espera el validador: "ts=<ts>,v1=<hmac>"
        String ts = "1700000000";
        String paymentId = "12345";
        String requestId = "req-abc";
        String data = "id:" + paymentId + ";request-id:" + requestId + ";ts:" + ts + ";";
        String hmac = computeHmac("test_secret_key", data);
        String signature = "ts=" + ts + ",v1=" + hmac;

        assertDoesNotThrow(() ->
                validator.validar(signature, requestId, paymentId)
        );
    }

    @Test
    @DisplayName("validarFirmaWebhook: firma inválida → lanza SecurityException")
    void validarFirmaWebhook_firmaInvalida_deberiaLanzarExcepcion() {
        com.tallersoft.security.MercadoPagoWebhookValidator validator =
                new com.tallersoft.security.MercadoPagoWebhookValidator();
        ReflectionTestUtils.setField(validator, "webhookSecret", "test_secret_key");

        String badSignature = "ts=1700000000,v1=hashincorrecto000000";
        assertThrows(SecurityException.class, () ->
                validator.validar(badSignature, "req-abc", "12345")
        );
    }

    // ─── MercadoPagoService: error sin accessToken ───────────────────────────

    @Test
    @DisplayName("generarPreferencia: accessToken vacío → lanza RuntimeException")
    void generarPreferencia_sinAccessToken_deberiaLanzarRuntimeException() {
        MercadoPagoService service = new MercadoPagoService();
        ReflectionTestUtils.setField(service, "accessToken", "");
        ReflectionTestUtils.setField(service, "webhookBaseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(service, "sandbox", true);
        ReflectionTestUtils.setField(service, "userId", "0");
        ReflectionTestUtils.setField(service, "posExternalId", "tallersoftcaja01");
        ReflectionTestUtils.setField(service, "qrImageUrl", "");

        assertThrows(RuntimeException.class, () ->
                service.generarPreferencia(1L, new BigDecimal("1000"), "Test orden")
        );
    }

    @Test
    @DisplayName("cargarOrdenEnCaja: URL de POS no accesible → lanza RuntimeException")
    void cargarOrdenEnCaja_sinConexion_deberiaLanzarRuntimeException() {
        MercadoPagoService service = new MercadoPagoService();
        ReflectionTestUtils.setField(service, "accessToken", "test_token");
        ReflectionTestUtils.setField(service, "webhookBaseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(service, "sandbox", true);
        ReflectionTestUtils.setField(service, "userId", "0");
        ReflectionTestUtils.setField(service, "posExternalId", "tallersoftcaja01");
        ReflectionTestUtils.setField(service, "qrImageUrl", "");

        assertThrows(RuntimeException.class, () ->
                service.cargarOrdenEnCaja(1L, new BigDecimal("5000"), "Reparación orden #1")
        );
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private String computeHmac(String secret, String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec =
                    new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
