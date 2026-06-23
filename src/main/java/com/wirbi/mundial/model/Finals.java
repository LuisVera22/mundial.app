package com.wirbi.mundial.model;

/**
 * Resultados finales del torneo que resuelven los picks globales:
 * campeón (Team.code) y goleador / Bota de Oro (Player.id).
 */
public record Finals(String champion, String scorer) {
    public static Finals empty() {
        return new Finals(null, null);
    }
}
