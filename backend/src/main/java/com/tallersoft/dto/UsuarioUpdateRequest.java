package com.tallersoft.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class UsuarioUpdateRequest {

    @NotBlank(message = "El nombre es requerido")
    private String nombre;

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    private String email;

    private String telefono;

    // Optional — only updated when not blank
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    // Required when changing own password; not needed when admin changes another user's password
    private String currentPassword;

    @NotNull(message = "El rol es requerido")
    private String rol;
}
