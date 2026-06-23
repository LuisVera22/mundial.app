package com.wirbi.mundial.service;

import com.wirbi.mundial.config.TournamentProperties;
import com.wirbi.mundial.core.MatchStatusService;
import com.wirbi.mundial.dto.MatchDto;
import com.wirbi.mundial.dto.MatchPredictionDto;
import com.wirbi.mundial.exception.ConflictException;
import com.wirbi.mundial.model.Match;
import com.wirbi.mundial.model.Prediction;
import com.wirbi.mundial.model.Stage;
import com.wirbi.mundial.model.Team;
import com.wirbi.mundial.model.User;
import com.wirbi.mundial.repository.MatchRepository;
import com.wirbi.mundial.repository.PredictionRepository;
import com.wirbi.mundial.repository.TeamRepository;
import com.wirbi.mundial.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-11T00:00:00Z");

    private final MatchRepository matches = mock(MatchRepository.class);
    private final TeamRepository teams = mock(TeamRepository.class);
    private final MatchStatusService status = new MatchStatusService(
            new TournamentProperties(NOW, NOW, NOW, NOW.plusSeconds(86400), 15, 60),
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final PredictionRepository predictions = mock(PredictionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final MatchService svc = new MatchService(matches, teams, status, predictions, users);

    private Match m(String id, String home, String away) {
        return new Match(id, home, away, NOW.plusSeconds(86400), Stage.GROUP, "D", "Stadium", null, null);
    }

    @Test
    void matchesForTeamFiltersHomeOrAway() {
        when(teams.findAll()).thenReturn(List.of(
                new Team("br", "Brasil", "E", null), new Team("ar", "Argentina", "D", null),
                new Team("fr", "Francia", "D", null)));
        when(matches.findAllByOrderByKickoffAsc()).thenReturn(List.of(
                m("1", "ar", "br"), m("2", "fr", "es"), m("3", "br", "fr")));

        List<MatchDto> brMatches = svc.matchesForTeam("br");

        assertEquals(2, brMatches.size());
        assertEquals(List.of("1", "3"), brMatches.stream().map(MatchDto::id).toList());
    }

    @Test
    void predictionsForLockedMatchRevealsAllSortedAndMarksMe() {
        // Partido ya iniciado (kickoff en el pasado) → LOCKED
        Match locked = new Match("9", "mx", "za", NOW.minusSeconds(600), Stage.GROUP, "A", "Azteca", null, null);
        when(matches.findById("9")).thenReturn(Optional.of(locked));
        when(users.findAll()).thenReturn(List.of(
                new User("u1", "Walter", 10, null, true, null, false),
                new User("u2", "Ana", 20, "a1.png", true, null, false)));
        when(predictions.findByMatchId("9")).thenReturn(List.of(
                new Prediction("p1", "u1", "9", 2, 0, NOW),
                new Prediction("p2", "u2", "9", 1, 1, NOW)));

        List<MatchPredictionDto> result = svc.predictionsFor("9", "u1");

        assertEquals(List.of("Ana", "Walter"), result.stream().map(MatchPredictionDto::name).toList());
        assertEquals(true, result.get(1).me()); // Walter consulta → su fila marcada
        assertEquals(false, result.get(0).me());
    }

    @Test
    void predictionsForOpenMatchAreHidden() {
        when(matches.findById("1")).thenReturn(Optional.of(m("1", "ar", "br"))); // kickoff mañana → OPEN
        assertThrows(ConflictException.class, () -> svc.predictionsFor("1", "u1"));
    }
}
