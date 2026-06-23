package com.wirbi.mundial.exception;

import java.time.Instant;
import java.util.List;

/** Cuerpo de error uniforme del API. */
public record ApiError(Instant timestamp, int status, String error, String message, List<String> details) {
}
