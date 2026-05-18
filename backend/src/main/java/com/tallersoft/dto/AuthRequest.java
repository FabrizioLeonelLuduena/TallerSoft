package com.tallersoft.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for login request
 */
@Data
public class AuthRequest {
    
    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    private String email;
    
    @NotBlank(message = "La contraseña es requerida")
    private String password;
}
