package com.wirbi.mundial.controller;

import com.wirbi.mundial.dto.FinalsRequest;
import com.wirbi.mundial.dto.MatchResultRequest;
import com.wirbi.mundial.exception.NotFoundException;
import com.wirbi.mundial.model.Finals;
import com.wirbi.mundial.model.LeaderboardSnapshot;
import com.wirbi.mundial.model.Match;
import com.wirbi.mundial.model.Score;
import com.wirbi.mundial.model.User;
import com.wirbi.mundial.repository.UserRepository;
import com.wirbi.mundial.service.LeaderboardService;
import com.wirbi.mundial.service.MatchService;
import com.wirbi.mundial.service.SyncResult;
import com.wirbi.mundial.service.SyncService;
import com.wirbi.mundial.service.TournamentStateService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoints administrativos. SIN seguridad todavía — se protegen al implementar
 * Entra ID (fase final). Disparados por Cloud Scheduler (sync) o manualmente.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final LeaderboardService leaderboard;
    private final SyncService sync;
    private final TournamentStateService tournamentState;
    private final MatchService matchService;
    private final UserRepository users;

    public AdminController(LeaderboardService leaderboard, SyncService sync,
                          TournamentStateService tournamentState, MatchService matchService,
                          UserRepository users) {
        this.leaderboard = leaderboard;
        this.sync = sync;
        this.tournamentState = tournamentState;
        this.matchService = matchService;
        this.users = users;
    }

    /** Sincroniza fixtures/resultados/campeón (+goleador) desde football-data.org. */
    @PostMapping("/sync")
    public SyncResult sync() {
        return sync.sync();
    }

    /** Fija manualmente los finales (p. ej. el goleador si /scorers no está en Free). */
    @PutMapping("/finals")
    public Finals setFinals(@RequestBody FinalsRequest body) {
        Finals finals = new Finals(body.champion(), body.scorer());
        tournamentState.setFinals(finals);
        return finals;
    }

    /**
     * Fija a mano el marcador final de un partido (respaldo cuando el proveedor
     * no expone el resultado). El sync no lo pisará: conserva los resultados ya
     * guardados. Ambos null → limpia el resultado.
     */
    @PutMapping("/matches/{id}/result")
    public Map<String, Object> setMatchResult(@PathVariable String id,
                                               @RequestBody MatchResultRequest body) {
        Match m = matchService.setResult(id, body.home(), body.away());
        Score r = m.getResult();
        Map<String, Object> out = new HashMap<>();
        out.put("id", m.getId());
        out.put("result", r != null ? Map.of("home", r.home(), "away", r.away()) : null);
        return out;
    }

    /**
     * Enrola (o saca) a un usuario del piloto beta (la Tribuna). Para armar la
     * población muestra sin tocar Mongo a mano:
     *   PUT /api/admin/users/{id}/beta  body: {"enabled": true}
     */
    @PutMapping("/users/{id}/beta")
    public Map<String, Object> setBetaAccess(@PathVariable String id,
                                             @RequestBody Map<String, Boolean> body) {
        User u = users.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + id));
        u.setBetaAccess(Boolean.TRUE.equals(body.get("enabled")));
        users.save(u);
        return Map.of("id", u.getId(), "name", u.getName(), "betaAccess", u.isBetaAccess());
    }

    /** Cierra la jornada: congela el ranking actual como snapshot (base de los deltas). */
    @PostMapping("/snapshots")
    public Map<String, Object> takeSnapshot() {
        LeaderboardSnapshot snap = leaderboard.takeSnapshot();
        return Map.of("round", snap.getRound(), "takenAt", snap.getTakenAt(),
                "participants", snap.getRanks().size());
    }
}
