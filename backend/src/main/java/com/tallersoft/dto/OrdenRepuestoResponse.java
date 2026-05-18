package com.tallersoft.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrdenRepuestoResponse {
    private Long id;
    private Long repuestoId;
    private String nombreRepuesto;
    private Integer cantidad;
    private BigDecimal precioUnit;
    private BigDecimal total;
}
