package com.wirbi.mundial.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Cuerpo para crear/actualizar un pronóstico de marcador (0–99, sin negativos). */
public record PredictionRequest(
        @Min(0) @Max(99) int home,
        @Min(0) @Max(99) int away
) {
}
