package com.tallersoft.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for equipment creation/update request
 */
@Data
public class EquipoRequest {
    
    @NotNull(message = "El cliente ID es requerido")
    private Long clienteId;
    
    @NotBlank(message = "El tipo de equipo es requerido")
    private String tipo;
    
    private String marca;
    
    private String modelo;
    
    private String numeroSerie;
    
    private String observaciones;
}
