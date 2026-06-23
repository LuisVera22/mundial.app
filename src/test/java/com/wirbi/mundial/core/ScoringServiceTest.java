package com.wirbi.mundial.core;

import com.wirbi.mundial.model.Outcome;
import com.wirbi.mundial.model.Score;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoringServiceTest {

    private final ScoringService s = new ScoringService();

    @Test
    void exactScoreGivesThree() {
        assertEquals(3, s.scoreMatch(new Score(2, 1), new Score(2, 1)));
    }

    @Test
    void correctTrendHomeGivesOne() {
        assertEquals(1, s.scoreMatch(new Score(2, 0), new Score(3, 0)));
    }

    @Test
    void correctTrendDrawGivesOne() {
        assertEquals(1, s.scoreMatch(new Score(0, 0), new Score(1, 1)));
    }

    @Test
    void missGivesZero() {
        // pred = HOME, result = DRAW
        assertEquals(0, s.scoreMatch(new Score(1, 0), new Score(2, 2)));
    }

    @Test
    void nullPredOrResultGivesZero() {
        assertEquals(0, s.scoreMatch(null, new Score(1, 0)));
        assertEquals(0, s.scoreMatch(new Score(1, 0), null));
    }

    @Test
    void outcomeIsDerivedCorrectly() {
        assertEquals(Outcome.HOME, s.outcome(new Score(2, 1)));
        assertEquals(Outcome.AWAY, s.outcome(new Score(0, 3)));
        assertEquals(Outcome.DRAW, s.outcome(new Score(1, 1)));
    }

    @Test
    void hitLabelThresholds() {
        assertEquals(HitLabel.EXACT, s.matchHitLabel(3));
        assertEquals(HitLabel.TREND, s.matchHitLabel(1));
        assertEquals(HitLabel.MISS, s.matchHitLabel(0));
    }
}
