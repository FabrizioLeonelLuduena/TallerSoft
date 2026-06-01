package com.tallersoft.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RepuestoResponse {
    private Long id;
    private String nombre;
    private String categoria;
    private BigDecimal precio;
    private Integer stockActual;
    private Integer stockMinimo;
    private Boolean critico;
    private LocalDateTime createdAt;
}
