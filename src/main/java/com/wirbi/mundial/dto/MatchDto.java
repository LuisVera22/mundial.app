package com.wirbi.mundial.dto;

import java.time.Instant;

/**
 * Partido para el feed. {@code status} ya viene derivado del servidor
 * (OPEN/URGENT/LOCKED/FINISHED) y {@code editable} indica si se puede pronosticar.
 */
public record MatchDto(
        String id,
        TeamRef home,
        TeamRef away,
        Instant kickoff,
        String stage,
        String stageKey,
        String group,
        String venue,
        String status,
        boolean editable,
        ScoreDto result,
        ScoreDto liveScore,   // marcador en curso (display); null si no aplica
        String liveStatus     // IN_PLAY / PAUSED; null si no está en vivo
) {
}
