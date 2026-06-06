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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebhookControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private CobrosService cobrosService;

    @Test
    @DisplayName("POST /api/pagos/webhook sin JWT → accesible (no retorna 401 ni 403)")
    void webhook_sinJwt_deberiaSerPublico() throws Exception {
        mockMvc.perform(post("/api/pagos/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature", "ts=1700000000,v1=fakesig")
                .header("X-Request-Id", "req-123")
                .content("{\"type\":\"payment\",\"data\":{\"id\":\"12345\"}}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertNotEquals(401, status, "El webhook no debe requerir JWT");
                    assertNotEquals(403, status, "El webhook no debe requerir JWT");
                });
    }

    @Test
    @DisplayName("POST /api/pagos/webhook sin cabeceras de firma → error de validación (4xx o 5xx)")
    void webhook_sinFirma_deberiaRetornarError() throws Exception {
        mockMvc.perform(post("/api/pagos/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"payment\",\"data\":{\"id\":\"12345\"}}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(
                        status >= 400,
                        "Sin firma debe retornar error (4xx o 5xx), pero fue: " + status
                    );
                });
    }
}
