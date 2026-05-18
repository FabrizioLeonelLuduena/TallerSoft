package com.tallersoft.dto;

import com.tallersoft.model.EstadoOrden;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CambiarEstadoRequest {
    @NotNull(message = "nuevoEstado es requerido")
    private EstadoOrden nuevoEstado;
}
