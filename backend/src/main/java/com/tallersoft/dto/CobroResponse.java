package com.tallersoft.dto;

import com.tallersoft.model.EstadoPago;
import com.tallersoft.model.MedioPago;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CobroResponse {

    private Long id;
    private Long ordenId;
    private String clienteNombre;
    private BigDecimal monto;
    private BigDecimal montoRecibido;
    private BigDecimal vuelto;
    private MedioPago medioPago;
    private EstadoPago estadoPago;
    private String mpLinkPago;
    private LocalDateTime createdAt;
}
