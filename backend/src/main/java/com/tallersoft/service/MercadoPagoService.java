package com.tallersoft.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MercadoPagoService {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${webhook.base-url}")
    private String webhookBaseUrl;

    @Value("${mercadopago.sandbox:true}")
    private boolean sandbox;

    public String generarLinkPago(Long ordenId, BigDecimal monto, String descripcion) {
        try {
            MercadoPagoConfig.setAccessToken(accessToken);

            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title(descripcion)
                    .quantity(1)
                    .unitPrice(monto)
                    .build();

            PreferenceRequest request = PreferenceRequest.builder()
                    .items(List.of(item))
                    .externalReference(String.valueOf(ordenId))
                    .notificationUrl(webhookBaseUrl + "/api/pagos/webhook")
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(request);

            String link = sandbox ? preference.getSandboxInitPoint() : preference.getInitPoint();
            log.info("Link de pago generado para orden {}: {}", ordenId, link);
            return link;

        } catch (Exception e) {
            log.error("Error generando link de pago para orden {}: {}", ordenId, e.getMessage());
            throw new RuntimeException("Error al generar el link de pago con MercadoPago: " + e.getMessage(), e);
        }
    }

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
}
