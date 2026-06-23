package com.wirbi.mundial.core;

import com.wirbi.mundial.config.TournamentProperties;
import com.wirbi.mundial.model.Finals;
import com.wirbi.mundial.model.GlobalPicks;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Reglas de las predicciones globales (campeón / goleador):
 *  - VALOR ESCALONADO POR FASE: el valor de una apuesta lo fija la fase en que
 *    se confirma (savedAt). Premia la convicción temprana.
 *      · Fase de grupos      → campeón 15 · goleador 6
 *      · Dieciseisavos (R32) → campeón 10 · goleador 4
 *      · Octavos (R16)       → campeón  5 · goleador 2
 *  - El valor se "congela" en el savedAt de cada pick (por campo).
 *  - Bloqueo de edición tras picksDeadlineAt (fin de octavos).
 */
@Service
public class GlobalPicksService {

    // Valores por fase (grupos / dieciseisavos / octavos).
    static final int CHAMPION_GROUP = ScoringService.CHAMPION; // 15
    static final int CHAMPION_R32 = 10;
    static final int CHAMPION_R16 = 5;
    static final int SCORER_GROUP = ScoringService.SCORER;     // 6
    static final int SCORER_R32 = 4;
    static final int SCORER_R16 = 2;

    private final TournamentProperties props;
    private final Clock clock;

    public GlobalPicksService(TournamentProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;
    }

    /** Fase del instante t: 0 = grupos, 1 = dieciseisavos, 2 = octavos o posterior. */
    private int phaseOf(Instant t) {
        if (t.isBefore(props.roundOf32At())) return 0;
        if (t.isBefore(props.roundOf16At())) return 1;
        return 2;
    }

    /** Valor del campeón según la fase de t (savedAt; o {@code now} en vivo si es null). */
    public int championValue(Instant savedAt, Instant now) {
        return switch (phaseOf(savedAt != null ? savedAt : now)) {
            case 0 -> CHAMPION_GROUP;
            case 1 -> CHAMPION_R32;
            default -> CHAMPION_R16;
        };
    }

    public int scorerValue(Instant savedAt, Instant now) {
        return switch (phaseOf(savedAt != null ? savedAt : now)) {
            case 0 -> SCORER_GROUP;
            case 1 -> SCORER_R32;
            default -> SCORER_R16;
        };
    }

    public int championValue(Instant savedAt) {
        return championValue(savedAt, now());
    }

    public int scorerValue(Instant savedAt) {
        return scorerValue(savedAt, now());
    }

    /** Valor mínimo posible (al confirmar en octavos). */
    public int championFloorValue() {
        return CHAMPION_R16;
    }

    public int scorerFloorValue() {
        return SCORER_R16;
    }

    /** ¿Edición de picks globales cerrada? (now >= picksDeadlineAt) */
    public boolean isLocked(Instant now) {
        return !now.isBefore(props.picksDeadlineAt());
    }

    public boolean isLocked() {
        return isLocked(now());
    }

    public Instant deadline() {
        return props.picksDeadlineAt();
    }

    /**
     * Puntos de las predicciones globales aplicando la devaluación: cada acierto
     * vale según el multiplicador del momento en que se fijó (savedAt por campo).
     */
    public int scoreGlobals(GlobalPicks picks, Finals finals) {
        if (picks == null || finals == null) return 0;
        int pts = 0;
        if (finals.champion() != null && finals.champion().equals(picks.champion())) {
            pts += championValue(picks.championSavedAt());
        }
        if (finals.scorer() != null && finals.scorer().equals(picks.scorer())) {
            pts += scorerValue(picks.scorerSavedAt());
        }
        return pts;
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
