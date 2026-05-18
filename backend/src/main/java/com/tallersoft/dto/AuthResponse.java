package com.tallersoft.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO for login response
 */
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String email;
    private String rol;
}
