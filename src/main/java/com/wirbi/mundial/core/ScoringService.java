package com.wirbi.mundial.core;

import com.wirbi.mundial.model.Outcome;
import com.wirbi.mundial.model.Score;
import org.springframework.stereotype.Service;

/**
 * Puntuación de partidos (port 1:1 de scoring.ts).
 *  - marcador exacto → 3
 *  - tendencia (mismo ganador/empate) → 1
 *  - sin acierto → 0
 */
@Service
public class ScoringService {

    public static final int EXACT = 3;
    public static final int TREND = 1;
    // Valor MÁXIMO de las apuestas globales (al fijarlas en fase de grupos).
    // El detalle por fase vive en GlobalPicksService.
    public static final int CHAMPION = 15;
    public static final int SCORER = 6;

    public Outcome outcome(Score score) {
        if (score.home() > score.away()) return Outcome.HOME;
        if (score.home() < score.away()) return Outcome.AWAY;
        return Outcome.DRAW;
    }

    /** Puntos de un partido dado el pronóstico y el resultado (null → 0). */
    public int scoreMatch(Score pred, Score result) {
        if (pred == null || result == null) return 0;
        if (pred.home() == result.home() && pred.away() == result.away()) return EXACT;
        if (outcome(pred) == outcome(result)) return TREND;
        return 0;
    }

    public HitLabel matchHitLabel(int pts) {
        if (pts >= EXACT) return HitLabel.EXACT;
        if (pts >= TREND) return HitLabel.TREND;
        return HitLabel.MISS;
    }
}
