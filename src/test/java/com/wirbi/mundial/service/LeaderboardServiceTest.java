package com.wirbi.mundial.service;

import com.wirbi.mundial.config.TournamentProperties;
import com.wirbi.mundial.model.Finals;
import com.wirbi.mundial.model.GlobalPicks;
import com.wirbi.mundial.model.LeaderboardSnapshot;
import com.wirbi.mundial.model.Match;
import com.wirbi.mundial.model.Prediction;
import com.wirbi.mundial.model.RankEntry;
import com.wirbi.mundial.model.Score;
import com.wirbi.mundial.model.Stage;
import com.wirbi.mundial.model.User;
import com.wirbi.mundial.repository.LeaderboardSnapshotRepository;
import com.wirbi.mundial.repository.MatchRepository;
import com.wirbi.mundial.repository.PredictionRepository;
import com.wirbi.mundial.repository.UserRepository;
import com.wirbi.mundial.core.GlobalPicksService;
import com.wirbi.mundial.core.ScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeaderboardServiceTest {

    private static final Instant START = Instant.parse("2026-06-11T00:00:00Z");
    private static final Instant DEADLINE = Instant.parse("2026-07-07T00:00:00Z");

    private final UserRepository users = mock(UserRepository.class);
    private final PredictionRepository preds = mock(PredictionRepository.class);
    private final MatchRepository matches = mock(MatchRepository.class);
    private final LeaderboardSnapshotRepository snaps = mock(LeaderboardSnapshotRepository.class);
    private final TournamentStateService state = mock(TournamentStateService.class);

    private final ScoringService scoring = new ScoringService();
    private final GlobalPicksService globalPicks = new GlobalPicksService(
            new TournamentProperties(START, START, START, DEADLINE, 15, 60),
            Clock.fixed(START, ZoneOffset.UTC));

    private final LeaderboardService svc = new LeaderboardService(
            users, preds, matches, snaps, scoring, globalPicks, state, Clock.fixed(START, ZoneOffset.UTC));

    @BeforeEach
    void setup() {
        when(state.finals()).thenReturn(Finals.empty());
        when(matches.findAll()).thenReturn(List.of(
                finished("m1", new Score(2, 1)),
                finished("m2", new Score(0, 0))));
        when(users.findAll()).thenReturn(List.of(
                user("me", "Tú"), user("u1", "Ana"), user("u2", "Beto")));
        when(preds.findAll()).thenReturn(List.of(
                pred("me", "m1", 2, 1),  // exacto → 3
                pred("u1", "m1", 1, 0))); // tendencia (local) → 1
        when(snaps.findTopByOrderByTakenAtDesc()).thenReturn(Optional.empty());
    }

    @Test
    void ordersByPointsDescWithStats() {
        List<RankedUser> r = svc.ranking();

        assertEquals("me", r.get(0).userId());
        assertEquals(3, r.get(0).points());
        assertEquals(1, r.get(0).rank());
        assertEquals(1, r.get(0).exactCount());

        assertEquals("u1", r.get(1).userId());
        assertEquals(1, r.get(1).points());
        assertEquals(1, r.get(1).trendCount());

        assertEquals("u2", r.get(2).userId());
        assertEquals(0, r.get(2).points());

        // sin snapshot previo → delta 0
        r.forEach(u -> assertEquals(0, u.delta()));
    }

    @Test
    void computesDeltaFromLatestSnapshot() {
        // jornada anterior: me era 2°, u1 era 1°
        when(snaps.findTopByOrderByTakenAtDesc()).thenReturn(Optional.of(
                new LeaderboardSnapshot("s1", START, 1,
                        List.of(new RankEntry("u1", 1, 5), new RankEntry("me", 2, 2)))));

        List<RankedUser> r = svc.ranking();

        assertEquals("me", r.get(0).userId());
        assertEquals(1, r.get(0).rank());
        assertEquals(1, r.get(0).delta());   // subió de 2 a 1 → +1 ▲

        assertEquals("u1", r.get(1).userId());
        assertEquals(2, r.get(1).rank());
        assertEquals(-1, r.get(1).delta());  // bajó de 1 a 2 → -1 ▼

        assertEquals(0, r.get(2).delta());   // u2 no estaba en el snapshot
    }

    private Match finished(String id, Score result) {
        return new Match(id, "a", "b", START, Stage.GROUP, "A", "venue", result, null);
    }

    private User user(String id, String name) {
        return new User(id, name, 84, null, true, GlobalPicks.empty(), false);
    }

    private Prediction pred(String userId, String matchId, int home, int away) {
        return new Prediction(null, userId, matchId, home, away, START);
    }
}
