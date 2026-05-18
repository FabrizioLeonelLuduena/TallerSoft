package com.tallersoft.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import com.tallersoft.model.Prioridad;
import lombok.Data;

@Data
public class OrdenTrabajoRequest {
    @NotNull(message = "equipoId es requerido")
    private Long equipoId;
    
    @NotNull(message = "clienteId es requerido")
    private Long clienteId;
    
    private Long tecnicoId;
    
    @NotBlank(message = "fallaReportada es requerida")
    private String fallaReportada;
    
    private Prioridad prioridad = Prioridad.NORMAL;
}
