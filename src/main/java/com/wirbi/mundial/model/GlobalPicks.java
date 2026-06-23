package com.wirbi.mundial.model;

import java.time.Instant;

/**
 * Predicciones globales de un usuario, embebidas en {@link User}.
 * Los timestamps por campo permiten la devaluación independiente: editar el
 * goleador no devalúa al campeón.
 */
public record GlobalPicks(
        String champion,        // Team.code
        String scorer,          // Player.id
        Instant championSavedAt,
        Instant scorerSavedAt
) {
    public static GlobalPicks empty() {
        return new GlobalPicks(null, null, null, null);
    }
}
