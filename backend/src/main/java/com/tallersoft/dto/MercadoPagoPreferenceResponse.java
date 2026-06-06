package com.tallersoft.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MercadoPagoPreferenceResponse {
    private String initPoint;
    private String qrCodeBase64;
}
