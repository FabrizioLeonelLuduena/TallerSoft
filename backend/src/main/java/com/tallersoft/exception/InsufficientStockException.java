package com.tallersoft.exception;

/**
 * Exception thrown when insufficient stock is available for an operation
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
