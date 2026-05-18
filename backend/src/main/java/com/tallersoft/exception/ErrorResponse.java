package com.tallersoft.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Standard error response format for all API errors
 */
@Data
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}
