package com.wirbi.mundial.service;

import com.wirbi.mundial.integration.FootballProvider;
import com.wirbi.mundial.integration.ProviderMatch;
import com.wirbi.mundial.integration.ProviderScorer;
import com.wirbi.mundial.integration.ProviderTeam;
import com.wirbi.mundial.model.Finals;
import com.wirbi.mundial.model.Player;
import com.wirbi.mundial.repository.MatchRepository;
import com.wirbi.mundial.repository.PlayerRepository;
import com.wirbi.mundial.repository.TeamRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncServiceTest {

    private final FootballProvider provider = mock(FootballProvider.class);
    private final MatchRepository matches = mock(MatchRepository.class);
    private final TeamRepository teams = mock(TeamRepository.class);
    private final PlayerRepository players = mock(PlayerRepository.class);
    private final TournamentStateService state = mock(TournamentStateService.class);
    private final LeaderboardService leaderboard = mock(LeaderboardService.class);
    private final ScorerService scorerService = mock(ScorerService.class);

    private final SyncService svc = new SyncService(provider, matches, teams, players, state, leaderboard, scorerService);

    private ProviderMatch pm(String id, String stage, String group, ProviderTeam h, ProviderTeam a,
                             Integer hs, Integer as, String winner) {
        return new ProviderMatch(id, Instant.parse("2026-06-11T16:00:00Z"), "FINISHED", stage, group,
                h, a, hs, as, winner, "Stadium");
    }

    @Test
    void skipsWhenProviderNotConfigured() {
        when(provider.isConfigured()).thenReturn(false);
        SyncResult r = svc.sync();
        assertTrue(r.skipped());
    }

    @Test
    void syncsMatchesDerivesChampionAndScorer() {
        when(provider.isConfigured()).thenReturn(true);
        when(teams.findById(anyString())).thenReturn(Optional.empty());
        when(teams.findAll()).thenReturn(List.of());
        when(state.finals()).thenReturn(Finals.empty());
        when(players.findAll()).thenReturn(List.of(new Player("p-mbappe", "Kylian Mbappé", "fr", null, 1)));
        when(provider.fetchMatches()).thenReturn(List.of(
                pm("1", "GROUP_STAGE", "GROUP_A",
                        new ProviderTeam("760", "Mexico", "MEX"), new ProviderTeam("3", "Croatia", "CRO"),
                        2, 1, "HOME_TEAM"),
                pm("100", "FINAL", null,
                        new ProviderTeam("1", "Brazil", "BRA"), new ProviderTeam("2", "France", "FRA"),
                        3, 1, "HOME_TEAM")));
        when(provider.fetchScorers()).thenReturn(List.of(
                new ProviderScorer("9", "Kylian Mbappé", "FRA", 7, 2, 1, 3)));

        SyncResult r = svc.sync();

        assertFalse(r.skipped());
        assertEquals(2, r.matchesSynced());
        assertEquals("br", r.champion());        // ganador del FINAL (BRA → br)
        assertEquals("p-mbappe", r.scorer());     // goleador resuelto por nombre
        assertTrue(r.scorersAvailable());
        verify(state).setFinals(new Finals("br", "p-mbappe"));
    }

    @Test
    void doesNotScoreScorerBeforeTournamentEnds() {
        when(provider.isConfigured()).thenReturn(true);
        when(teams.findById(anyString())).thenReturn(Optional.empty());
        when(teams.findAll()).thenReturn(List.of());
        // Goleador provisional fijado por un sync anterior (bug): debe limpiarse.
        when(state.finals()).thenReturn(new Finals(null, "p-messi"));
        when(provider.fetchMatches()).thenReturn(List.of(
                pm("1", "GROUP_STAGE", "GROUP_A",
                        new ProviderTeam("760", "Argentina", "ARG"), new ProviderTeam("3", "Croatia", "CRO"),
                        2, 1, "HOME_TEAM")));
        // El proveedor reporta un líder de goleo, pero en grupos NO debe puntuar.
        when(provider.fetchScorers()).thenReturn(List.of(
                new ProviderScorer("10", "Lionel Messi", "ARG", 5, 1, 0, 3)));

        SyncResult r = svc.sync();

        assertFalse(r.skipped());
        assertNull(r.champion());                       // sin FINAL: torneo en curso
        assertNull(r.scorer());                         // la Bota de Oro no se resuelve aún
        verify(state).setFinals(new Finals(null, null)); // limpia el goleador provisional
    }

    @Test
    void keepsExistingScorerWhenProviderHasNoScorers() {
        when(provider.isConfigured()).thenReturn(true);
        when(teams.findById(anyString())).thenReturn(Optional.empty());
        when(teams.findAll()).thenReturn(List.of());
        when(state.finals()).thenReturn(new Finals(null, "p-messi")); // goleador fijado manual
        when(provider.fetchMatches()).thenReturn(List.of(
                pm("100", "FINAL", null,
                        new ProviderTeam("1", "Argentina", "ARG"), new ProviderTeam("2", "France", "FRA"),
                        4, 2, "HOME_TEAM")));
        when(provider.fetchScorers()).thenReturn(List.of()); // Free sin /scorers

        SyncResult r = svc.sync();

        assertEquals("ar", r.champion());
        assertFalse(r.scorersAvailable());
        verify(state).setFinals(new Finals("ar", "p-messi")); // no pisa el manual
    }
}
