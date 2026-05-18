package com.tallersoft.exception;

/**
 * Exception thrown when attempting to change an order to LISTO without a diagnosis
 */
public class MissingDiagnosticException extends RuntimeException {
    public MissingDiagnosticException(String message) {
        super(message);
    }
}
