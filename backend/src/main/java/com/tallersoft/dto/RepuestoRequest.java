package com.tallersoft.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RepuestoRequest {
    @NotBlank(message = "nombre es requerido")
    private String nombre;
    
    private String categoria;
    
    @NotNull(message = "precio es requerido")
    @DecimalMin(value = "0.01", message = "precio debe ser mayor a 0")
    private BigDecimal precio;
    
    @Min(value = 0, message = "stockActual no puede ser negativo")
    private Integer stockActual = 0;

    @Min(value = 0, message = "stockMinimo no puede ser negativo")
    private Integer stockMinimo = 5;

    @Min(value = 0, message = "stockBajo no puede ser negativo")
    private Integer stockBajo = 10;
}
