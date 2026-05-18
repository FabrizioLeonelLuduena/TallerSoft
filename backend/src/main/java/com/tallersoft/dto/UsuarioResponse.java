package com.tallersoft.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for user response
 */
@Data
@AllArgsConstructor
public class UsuarioResponse {
    private Long id;
    private String nombre;
    private String email;
    private String rol;
    private boolean activo;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
