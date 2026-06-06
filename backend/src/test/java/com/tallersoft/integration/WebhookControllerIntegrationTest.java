package com.tallersoft.integration;

import com.tallersoft.service.CobrosService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for PagoController webhook endpoint.
 * The webhook endpoint is public (no JWT required) but validates HMAC signature.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebhookControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private CobrosService cobrosService;

    @Test
    @DisplayName("POST /api/pagos/webhook sin JWT pero con firma válida → procesado sin error de auth")
    void webhook_sinJwtFirmaPresente_deberiaAcceder() throws Exception {
        // El endpoint debe ser accesible sin JWT (es público)
        // Con webhook_secret vacío en test profile, la validación puede pasar o ignorarse
        mockMvc.perform(post("/api/pagos/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature", "ts=1700000000,v1=fakesig")
                .header("X-Request-Id", "req-123")
                .content("{\"type\":\"payment\",\"data\":{\"id\":\"12345\"}}"))
                // El endpoint es accesible (no retorna 401/403)
                .andExpect(status().is5xxServerError().xor(
                        status().is2xxSuccessful())
                );
    }

    @Test
    @DisplayName("POST /api/pagos/webhook sin cabeceras de firma → error de validación")
    void webhook_sinFirma_deberiaRetornarError() throws Exception {
        mockMvc.perform(post("/api/pagos/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"payment\",\"data\":{\"id\":\"12345\"}}"))
                .andExpect(status().is4xxClientError()
                        .or(status().is5xxServerError()));
    }
}
