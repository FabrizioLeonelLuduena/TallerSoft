package com.tallersoft.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO for equipment response
 */
@Data
@AllArgsConstructor
public class EquipoResponse {
    private Long id;
    private Long clienteId;
    private String tipo;
    private String marca;
    private String modelo;
    private String numeroSerie;
    private String observaciones;
}
