package com.tallersoft.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CajaDiariaResponse {

    private LocalDate fecha;
    private BigDecimal totalDia;
    private Integer cantidadOrdenes;
    private BigDecimal totalEfectivo;
    private BigDecimal totalTarjeta;
    private BigDecimal totalMercadoPago;
    private List<CobroResponse> cobrosDelDia;
}
