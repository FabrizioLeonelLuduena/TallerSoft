package com.tallersoft.exception;

/**
 * Exception thrown when an entity is not found in the database
 */
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
