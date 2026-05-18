package com.tallersoft.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DiagnosticoRequest {
    @NotBlank(message = "diagnostico es requerido")
    private String diagnostico;
}
