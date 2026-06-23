package com.wirbi.mundial.core;

import com.wirbi.mundial.config.TournamentProperties;
import com.wirbi.mundial.model.Finals;
import com.wirbi.mundial.model.GlobalPicks;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalPicksServiceTest {

    private static final Instant START = Instant.parse("2026-06-11T00:00:00Z");
    private static final Instant R32 = Instant.parse("2026-06-28T00:00:00Z");   // dieciseisavos
    private static final Instant R16 = Instant.parse("2026-07-04T00:00:00Z");   // octavos
    private static final Instant DEADLINE = Instant.parse("2026-07-07T22:00:00Z");

    private final TournamentProperties props =
            new TournamentProperties(START, R32, R16, DEADLINE, 15, 60);
    private final GlobalPicksService svc =
            new GlobalPicksService(props, Clock.fixed(START, ZoneOffset.UTC));

    @Test
    void groupPhaseValue() {
        // fijado en fase de grupos (o antes del inicio) → valor máximo
        assertEquals(15, svc.championValue(START, START));
        assertEquals(6, svc.scorerValue(START, START));
        assertEquals(15, svc.championValue(START.minus(2, ChronoUnit.DAYS), START));
        assertEquals(15, svc.championValue(R32.minus(1, ChronoUnit.SECONDS), START));
    }

    @Test
    void roundOf32Value() {
        assertEquals(10, svc.championValue(R32, START));
        assertEquals(4, svc.scorerValue(R32, START));
        assertEquals(10, svc.championValue(R16.minus(1, ChronoUnit.SECONDS), START));
    }

    @Test
    void roundOf16Value() {
        assertEquals(5, svc.championValue(R16, START));
        assertEquals(2, svc.scorerValue(R16, START));
        // en/desde el deadline sigue valiendo lo de octavos (piso)
        assertEquals(5, svc.championValue(DEADLINE, START));
        assertEquals(5, svc.championFloorValue());
        assertEquals(2, svc.scorerFloorValue());
    }

    @Test
    void liveValueUsesNowWhenNoSavedAt() {
        assertEquals(15, svc.championValue(null, START));   // grupos
        assertEquals(10, svc.championValue(null, R32));     // dieciseisavos
        assertEquals(5, svc.championValue(null, R16));      // octavos
    }

    @Test
    void lockedOnlyAtOrAfterDeadline() {
        assertFalse(svc.isLocked(R16));
        assertTrue(svc.isLocked(DEADLINE));
        assertTrue(svc.isLocked(DEADLINE.plusSeconds(1)));
    }

    @Test
    void scoreGlobalsUsesPerFieldSavedAt() {
        Finals finals = new Finals("br", "p-vinicius");
        // campeón fijado en grupos (15) + goleador fijado en octavos (2) = 17
        GlobalPicks picks = new GlobalPicks("br", "p-vinicius", START, R16);
        assertEquals(17, svc.scoreGlobals(picks, finals));
    }

    @Test
    void scoreGlobalsZeroWhenWrongOrMissing() {
        Finals finals = new Finals("br", "p-vinicius");
        assertEquals(0, svc.scoreGlobals(new GlobalPicks("ar", "p-messi", START, START), finals));
        assertEquals(0, svc.scoreGlobals(GlobalPicks.empty(), finals));
        assertEquals(0, svc.scoreGlobals(new GlobalPicks("br", null, START, null), Finals.empty()));
    }
}
