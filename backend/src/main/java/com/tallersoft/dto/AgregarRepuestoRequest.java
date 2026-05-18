package com.tallersoft.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AgregarRepuestoRequest {
    @NotNull(message = "repuestoId es requerido")
    private Long repuestoId;
    
    @NotNull(message = "cantidad es requerida")
    @Min(value = 1, message = "cantidad debe ser mayor a 0")
    private Integer cantidad;
}
