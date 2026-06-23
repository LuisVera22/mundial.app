package com.wirbi.mundial.dto;

/**
 * Pronóstico de un usuario para un partido, visible para todos recién cuando
 * el partido cierra (LOCKED/FINISHED). {@code me} marca la fila del usuario
 * que consulta, para resaltarla en la UI.
 */
public record MatchPredictionDto(String name, int hue, String avatar, int home, int away, boolean me) {
}
