package com.tallersoft.dto;

import com.tallersoft.model.MedioPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CobrarOrdenRequest {

    @NotNull(message = "El ID de la orden es requerido")
    private Long ordenId;

    @NotNull(message = "El monto es requerido")
    @Positive(message = "El monto debe ser positivo")
    private BigDecimal monto;

    private BigDecimal montoRecibido;

    @NotNull(message = "El medio de pago es requerido")
    private MedioPago medioPago;
}
