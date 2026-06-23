package com.wirbi.mundial.dto;

import java.time.Instant;

/**
 * Picks globales con sus valores actuales ya calculados (devaluados) y el estado
 * de bloqueo/deadline. El front solo renderiza.
 */
public record GlobalPicksDto(
        TeamRef champion,
        Integer championValue,   // valor congelado actual (null si no hay pick)
        PlayerDto scorer,
        Integer scorerValue,
        int championMax,
        int championFloor,
        int scorerMax,
        int scorerFloor,
        boolean locked,
        Instant deadline
) {
}
