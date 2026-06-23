package com.wirbi.mundial.service;

import com.wirbi.mundial.model.Match;
import com.wirbi.mundial.model.MatchStatus;
import com.wirbi.mundial.model.Score;
import com.wirbi.mundial.model.Team;
import com.wirbi.mundial.model.User;
import com.wirbi.mundial.repository.MatchRepository;
import com.wirbi.mundial.repository.PredictionRepository;
import com.wirbi.mundial.repository.TeamRepository;
import com.wirbi.mundial.repository.UserRepository;
import com.wirbi.mundial.core.MatchStatusService;
import com.wirbi.mundial.dto.MatchDto;
import com.wirbi.mundial.dto.MatchPredictionDto;
import com.wirbi.mundial.dto.ScoreDto;
import com.wirbi.mundial.dto.TeamRef;
import com.wirbi.mundial.exception.ConflictException;
import com.wirbi.mundial.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Lista de partidos con estado derivado del tiempo, lista para el feed. */
@Service
public class MatchService {

    private final MatchRepository matches;
    private final TeamRepository teams;
    private final MatchStatusService statusService;
    private final PredictionRepository predictions;
    private final UserRepository users;

    public MatchService(MatchRepository matches, TeamRepository teams, MatchStatusService statusService,
                        PredictionRepository predictions, UserRepository users) {
        this.matches = matches;
        this.teams = teams;
        this.statusService = statusService;
        this.predictions = predictions;
        this.users = users;
    }

    public List<MatchDto> list() {
        Map<String, Team> teamMap = teams.findAll().stream()
                .collect(Collectors.toMap(Team::getCode, Function.identity()));
        return matches.findAllByOrderByKickoffAsc().stream()
                .map(m -> toDto(m, teamMap))
                .toList();
    }

    /**
     * Fija (o limpia) el marcador final de un partido a mano. Respaldo para
     * cuando el proveedor no expone el resultado. Ambos null → limpia.
     */
    public Match setResult(String matchId, Integer home, Integer away) {
        Match m = matches.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));
        m.setResult(home != null && away != null ? new Score(home, away) : null);
        return matches.save(m);
    }

    /**
     * Pronósticos de todos los participantes para un partido. Solo visibles
     * cuando el pronóstico ya cerró (LOCKED/FINISHED): antes serían spoilers
     * copiables. Ordenados por nombre; {@code me} marca al usuario que consulta.
     */
    public List<MatchPredictionDto> predictionsFor(String matchId, String currentUserId) {
        Match m = matches.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Partido no encontrado: " + matchId));
        if (statusService.isEditable(m)) {
            throw new ConflictException("Los pronósticos se revelan cuando cierra el partido.");
        }
        Map<String, User> userMap = users.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return predictions.findByMatchId(matchId).stream()
                .map(p -> {
                    User u = userMap.get(p.getUserId());
                    if (u == null) return null; // predicción huérfana (usuario borrado)
                    return new MatchPredictionDto(u.getName(), u.getHue(), u.getAvatar(),
                            p.getHome(), p.getAway(), u.getId().equals(currentUserId));
                })
                .filter(dto -> dto != null)
                .sorted(Comparator.comparing(MatchPredictionDto::name))
                .toList();
    }

    /** Partidos de una selección (local o visitante), en orden cronológico. */
    public List<MatchDto> matchesForTeam(String code) {
        Map<String, Team> teamMap = teams.findAll().stream()
                .collect(Collectors.toMap(Team::getCode, Function.identity()));
        return matches.findAllByOrderByKickoffAsc().stream()
                .filter(m -> code.equals(m.getHome()) || code.equals(m.getAway()))
                .map(m -> toDto(m, teamMap))
                .toList();
    }

    private MatchDto toDto(Match m, Map<String, Team> teamMap) {
        MatchStatus status = statusService.statusOf(m);
        Score r = m.getResult();
        // Solo mostramos el marcador en vivo si el partido aún no finalizó
        // (al finalizar, manda `result`).
        Score live = status == MatchStatus.FINISHED ? null : m.getLiveScore();
        return new MatchDto(
                m.getId(),
                ref(m.getHome(), teamMap),
                ref(m.getAway(), teamMap),
                m.getKickoff(),
                m.getStage() != null ? m.getStage().label() : null,
                m.getStage() != null ? m.getStage().name() : null,
                m.getGroup(),
                m.getVenue(),
                status.name(),
                statusService.isEditable(status),
                r != null ? new ScoreDto(r.home(), r.away()) : null,
                live != null ? new ScoreDto(live.home(), live.away()) : null,
                m.getLiveStatus()
        );
    }

    private TeamRef ref(String code, Map<String, Team> teamMap) {
        if (code == null) return null;
        Team t = teamMap.get(code);
        return new TeamRef(code, t != null ? t.getName() : code);
    }
}
