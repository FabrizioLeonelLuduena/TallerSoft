package com.tallersoft.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for client creation/update request
 */
@Data
public class ClienteRequest {
    
    @NotBlank(message = "El nombre es requerido")
    private String nombre;
    
    private String telefono;
    
    @Email(message = "El email debe ser válido")
    private String email;
    
    private String direccion;
}
