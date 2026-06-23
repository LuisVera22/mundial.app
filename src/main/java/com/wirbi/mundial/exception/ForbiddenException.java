package com.wirbi.mundial.exception;

/** Recurso existente pero vedado para el usuario actual → 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
