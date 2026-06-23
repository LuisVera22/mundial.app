package com.wirbi.mundial.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/** Traduce excepciones a respuestas HTTP uniformes. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> forbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), List.of());
    }

    /** Ruta/recurso inexistente (p. ej. pegar a "/") → 404, no 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> noResource(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Ruta no encontrada. Las APIs viven bajo /api/...", List.of());
    }

    /** Método HTTP incorrecto (p. ej. GET a un endpoint POST) → 405, no 500. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        String allowed = ex.getSupportedHttpMethods() == null ? "" : ex.getSupportedHttpMethods().toString();
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                "Método " + ex.getMethod() + " no permitido. Métodos válidos: " + allowed, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Datos inválidos.", details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> generic(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno.", List.of(ex.getMessage()));
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status).body(
                new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, details));
    }
}
