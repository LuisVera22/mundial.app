package com.wirbi.mundial.integration;

import java.time.Instant;

/**
 * Partido normalizado desde el proveedor (independiente del JSON crudo).
 *  - status: SCHEDULED/TIMED/IN_PLAY/PAUSED/FINISHED…
 *  - winner: HOME_TEAM/AWAY_TEAM/DRAW/null (incluye penales) → para el campeón.
 *  - home/away y los scores pueden ser null (TBD / no finalizado).
 */
public record ProviderMatch(
        String providerId,
        Instant kickoff,
        String status,
        String stage,
        String group,
        ProviderTeam home,
        ProviderTeam away,
        Integer homeScore,
        Integer awayScore,
        String winner,
        String venue
) {
    public boolean finished() {
        return "FINISHED".equals(status);
    }

    /** En curso: en juego o en descanso (medio tiempo). */
    public boolean live() {
        return "IN_PLAY".equals(status) || "PAUSED".equals(status);
    }
}
