package com.tallersoft.exception;

/**
 * Exception thrown when an invalid state transition is attempted on an order
 */
public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
