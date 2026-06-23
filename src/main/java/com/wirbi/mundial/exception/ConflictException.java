package com.wirbi.mundial.exception;

/** Acción no permitida por el estado actual (p. ej. editar algo bloqueado) → HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
