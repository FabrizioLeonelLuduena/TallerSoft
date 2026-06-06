package com.tallersoft.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.tallersoft.dto.MercadoPagoPreferenceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/*
 * DIFERENCIA ENTRE MODALIDADES DE PAGO QR DE MERCADOPAGO
 *
 * 1. Checkout Pro (Preference)
 *    - Se crea una Preference via SDK → MP devuelve un initPoint (link)
 *    - Desde ese link se genera un QR con ZXing (contenido = URL del link)
 *    - El cliente debe abrir la app de MercadoPago para pagar
 *    - Limitación: NO es interoperable con otras billeteras
 *
 * 2. QR de Punto de Venta (POS) — instore API
 *    - El QR es una imagen estática asignada a un POS (terminal) de la sucursal
 *    - Antes de mostrarlo, se carga el monto vía PUT a /instore/orders/qr/...
 *    - El QR sigue el estándar EMVCo / Pago QR del BCRA
 *    - Funciona con cualquier billetera: Modo, Ualá, Naranja X, Personal Pay, etc.
 *    - Es el QR recomendado para uso presencial en caja
 */
@Service
@Slf4j
public class MercadoPagoService {

    private static final String MP_API_BASE = "https://api.mercadopago.com";

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${webhook.base-url}")
    private String webhookBaseUrl;

    @Value("${mercadopago.sandbox:true}")
    private boolean sandbox;

    @Value("${mercadopago.user-id:0}")
    private String userId;

    @Value("${mercadopago.pos-external-id:tallersoftcaja01}")
    private String posExternalId;

    @Value("${mercadopago.qr-image-url:}")
    private String qrImageUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // ── Checkout Pro ─────────────────────────────────────────────────────────

    public MercadoPagoPreferenceResponse generarPreferencia(Long ordenId, BigDecimal monto, String descripcion) {
        try {
            MercadoPagoConfig.setAccessToken(accessToken);

            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title(descripcion)
                    .quantity(1)
                    .unitPrice(monto)
                    .currencyId("ARS")
                    .build();

            PreferenceRequest.PreferenceRequestBuilder builder = PreferenceRequest.builder()
                    .items(List.of(item))
                    .externalReference(String.valueOf(ordenId));

            // MP rechaza notificationUrl con localhost — solo la incluimos si hay URL pública
            if (!webhookBaseUrl.contains("localhost") && !webhookBaseUrl.contains("127.0.0.1")) {
                builder.notificationUrl(webhookBaseUrl + "/api/pagos/webhook");
            } else {
                log.warn("WEBHOOK_BASE_URL es localhost — notificationUrl omitida. Usá ngrok para recibir webhooks.");
            }

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(builder.build());

            String link = sandbox ? preference.getSandboxInitPoint() : preference.getInitPoint();
            log.info("Preference generada para orden {}: {}", ordenId, link);

            return MercadoPagoPreferenceResponse.builder()
                    .initPoint(link)
                    .qrCodeBase64(generarQrBase64(link))
                    .build();

        } catch (MPApiException e) {
            log.error("MP API error para orden {} — status: {}, body: {}",
                    ordenId, e.getApiResponse().getStatusCode(), e.getApiResponse().getContent());
            throw new RuntimeException("Error MercadoPago (" + e.getApiResponse().getStatusCode() + "): "
                    + e.getApiResponse().getContent(), e);
        } catch (Exception e) {
            log.error("Error generando preference para orden {}: {}", ordenId, e.getMessage());
            throw new RuntimeException("Error al generar el pago con MercadoPago: " + e.getMessage(), e);
        }
    }

    // ── QR de Punto de Venta (POS) ────────────────────────────────────────────

    public String cargarOrdenEnCaja(Long ordenId, BigDecimal monto, String descripcion) {
        String url = MP_API_BASE + "/instore/orders/qr/seller/collectors/" + userId
                + "/pos/" + posExternalId + "/qrs";

        Map<String, Object> item = Map.of(
                "title", descripcion,
                "currency_id", "ARS",
                "unit_price", monto,
                "quantity", 1,
                "unit_measure", "unit",
                "total_amount", monto
        );

        Map<String, Object> body = Map.of(
                "external_reference", String.valueOf(ordenId),
                "title", descripcion,
                "description", descripcion,
                "total_amount", monto,
                "items", List.of(item)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
            log.info("Orden {} cargada en caja POS ({})", ordenId, posExternalId);
        } catch (Exception e) {
            log.error("Error cargando orden {} en caja POS: {}", ordenId, e.getMessage());
            throw new RuntimeException("Error al cargar la orden en el QR de caja: " + e.getMessage(), e);
        }

        return qrImageUrl;
    }

    // ── Consulta de pago ──────────────────────────────────────────────────────

    public Map<String, Object> consultarPago(String paymentId) {
        try {
            MercadoPagoConfig.setAccessToken(accessToken);

            PaymentClient client = new PaymentClient();
            Payment payment = client.get(Long.parseLong(paymentId));

            log.info("Pago consultado: id={}, status={}", paymentId, payment.getStatus());
            return Map.of(
                    "status", payment.getStatus(),
                    "externalReference", payment.getExternalReference() != null
                            ? payment.getExternalReference() : "",
                    "mpPaymentId", payment.getId().toString()
            );

        } catch (Exception e) {
            log.error("Error consultando pago {}: {}", paymentId, e.getMessage());
            throw new RuntimeException("Error al consultar el pago en MercadoPago: " + e.getMessage(), e);
        }
    }

    // ── Búsqueda de pago por orden (reconciliación activa) ───────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> buscarPagoPorOrden(Long ordenId) {
        String url = MP_API_BASE + "/v1/payments/search"
                + "?external_reference=" + ordenId
                + "&sort=date_created&criteria=desc&limit=1";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            if (response.getBody() == null) return null;

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.getBody().get("results");

            if (results == null || results.isEmpty()) return null;

            Map<String, Object> payment = results.get(0);
            String status = (String) payment.get("status");
            String mpId   = String.valueOf(payment.get("id"));

            log.info("Búsqueda pago para orden {}: mp_id={}, status={}", ordenId, mpId, status);
            return Map.of("status", status, "mpPaymentId", mpId);

        } catch (Exception e) {
            log.warn("No se pudo buscar pago para orden {}: {}", ordenId, e.getMessage());
            return null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generarQrBase64(String contenido) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(
                    contenido,
                    BarcodeFormat.QR_CODE,
                    300, 300,
                    Map.of(EncodeHintType.MARGIN, 1)
            );
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("Error generando QR base64: {}", e.getMessage());
            return null;
        }
    }
}
