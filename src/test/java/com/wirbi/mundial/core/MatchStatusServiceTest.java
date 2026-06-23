package com.wirbi.mundial.core;

import com.wirbi.mundial.config.TournamentProperties;
import com.wirbi.mundial.model.Match;
import com.wirbi.mundial.model.MatchStatus;
import com.wirbi.mundial.model.Score;
import com.wirbi.mundial.model.Stage;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchStatusServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-11T12:00:00Z");

    private final TournamentProperties props = new TournamentProperties(
            Instant.parse("2026-06-11T00:00:00Z"),
            Instant.parse("2026-06-28T00:00:00Z"),
            Instant.parse("2026-07-04T00:00:00Z"),
            Instant.parse("2026-07-07T22:00:00Z"),
            15, 60);
    private final MatchStatusService svc =
            new MatchStatusService(props, Clock.fixed(NOW, ZoneOffset.UTC));

    private Match matchInMinutes(long minutes, Score result) {
        return new Match("m1", "ar", "br", NOW.plusSeconds(minutes * 60),
                Stage.GROUP, "D", "Hard Rock Stadium", result, null);
    }

    @Test
    void finishedWhenResultPresent() {
        assertEquals(MatchStatus.FINISHED, svc.statusOf(matchInMinutes(120, new Score(1, 0)), NOW));
    }

    @Test
    void lockedAtOrUnder15Minutes() {
        assertEquals(MatchStatus.LOCKED, svc.statusOf(matchInMinutes(10, null), NOW));
        assertEquals(MatchStatus.LOCKED, svc.statusOf(matchInMinutes(15, null), NOW)); // límite inclusivo
    }

    @Test
    void urgentBetween15And60Minutes() {
        assertEquals(MatchStatus.URGENT, svc.statusOf(matchInMinutes(30, null), NOW));
        assertEquals(MatchStatus.URGENT, svc.statusOf(matchInMinutes(59, null), NOW));
    }

    @Test
    void openAt60MinutesAndBeyond() {
        assertEquals(MatchStatus.OPEN, svc.statusOf(matchInMinutes(60, null), NOW)); // 60 exacto → OPEN
        assertEquals(MatchStatus.OPEN, svc.statusOf(matchInMinutes(120, null), NOW));
    }

    @Test
    void usesInjectedClockByDefault() {
        assertEquals(MatchStatus.LOCKED, svc.statusOf(matchInMinutes(5, null)));
    }

    @Test
    void editableOnlyWhenOpenOrUrgent() {
        assertTrue(svc.isEditable(MatchStatus.OPEN));
        assertTrue(svc.isEditable(MatchStatus.URGENT));
        assertFalse(svc.isEditable(MatchStatus.LOCKED));
        assertFalse(svc.isEditable(MatchStatus.FINISHED));
    }
}
