package com.wirbi.mundial.service;

import com.wirbi.mundial.model.Finals;
import com.wirbi.mundial.model.LeaderboardSnapshot;
import com.wirbi.mundial.model.Match;
import com.wirbi.mundial.model.Prediction;
import com.wirbi.mundial.model.RankEntry;
import com.wirbi.mundial.model.Score;
import com.wirbi.mundial.model.User;
import com.wirbi.mundial.repository.LeaderboardSnapshotRepository;
import com.wirbi.mundial.repository.MatchRepository;
import com.wirbi.mundial.repository.PredictionRepository;
import com.wirbi.mundial.repository.UserRepository;
import com.wirbi.mundial.core.GlobalPicksService;
import com.wirbi.mundial.core.ScoringService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ranking autoritativo: puntos por usuario = Σ aciertos de partidos finalizados
 * + puntos de picks globales (con devaluación). Orden DESC; el delta ▲/▼ se
 * calcula contra el último snapshot.
 */
@Service
public class LeaderboardService {

    private final UserRepository users;
    private final PredictionRepository predictions;
    private final MatchRepository matches;
    private final LeaderboardSnapshotRepository snapshots;
    private final ScoringService scoring;
    private final GlobalPicksService globalPicks;
    private final TournamentStateService tournamentState;
    private final Clock clock;

    public LeaderboardService(UserRepository users, PredictionRepository predictions,
                              MatchRepository matches, LeaderboardSnapshotRepository snapshots,
                              ScoringService scoring, GlobalPicksService globalPicks,
                              TournamentStateService tournamentState, Clock clock) {
        this.users = users;
        this.predictions = predictions;
        this.matches = matches;
        this.snapshots = snapshots;
        this.scoring = scoring;
        this.globalPicks = globalPicks;
        this.tournamentState = tournamentState;
        this.clock = clock;
    }

    /** Ranking completo ordenado (rank 1..N) con delta contra el último snapshot. */
    public List<RankedUser> ranking() {
        Finals finals = tournamentState.finals();

        Map<String, Score> results = matches.findAll().stream()
                .filter(m -> m.getResult() != null)
                .collect(Collectors.toMap(Match::getId, Match::getResult));

        Map<String, List<Prediction>> byUser = predictions.findAll().stream()
                .collect(Collectors.groupingBy(Prediction::getUserId));

        Map<String, Integer> prevRanks = latestSnapshotRanks();

        // 1) calcular puntos por usuario
        List<RankedUser> scored = new ArrayList<>();
        for (User u : users.findAll()) {
            int matchPts = 0, exact = 0, trend = 0;
            for (Prediction p : byUser.getOrDefault(u.getId(), List.of())) {
                Score res = results.get(p.getMatchId());
                if (res == null) continue;
                int pts = scoring.scoreMatch(new Score(p.getHome(), p.getAway()), res);
                matchPts += pts;
                if (pts == ScoringService.EXACT) exact++;
                else if (pts == ScoringService.TREND) trend++;
            }
            int globalPts = globalPicks.scoreGlobals(u.getGlobalPicks(), finals);
            scored.add(new RankedUser(u.getId(), u.getName(), u.getHue(), u.getAvatar(), 0,
                    matchPts + globalPts, matchPts, globalPts, exact, trend, 0));
        }

        // 2) ordenar (puntos DESC, desempate por nombre) y asignar rank + delta
        scored.sort(Comparator.comparingInt(RankedUser::points).reversed()
                .thenComparing(RankedUser::name));
        List<RankedUser> ranked = new ArrayList<>(scored.size());
        for (int i = 0; i < scored.size(); i++) {
            RankedUser r = scored.get(i);
            int rank = i + 1;
            Integer prev = prevRanks.get(r.userId());
            int delta = prev != null ? prev - rank : 0;
            ranked.add(new RankedUser(r.userId(), r.name(), r.hue(), r.avatar(), rank, r.points(),
                    r.matchPoints(), r.globalPoints(), r.exactCount(), r.trendCount(), delta));
        }
        return ranked;
    }

    /** Fila del usuario indicado (o null si no existe en el ranking). */
    public RankedUser rowFor(String userId) {
        return ranking().stream().filter(r -> r.userId().equals(userId)).findFirst().orElse(null);
    }

    /** Número de jornada actual = round del último snapshot (0 si no hay). */
    public int currentRound() {
        return snapshots.findTopByOrderByTakenAtDesc().map(LeaderboardSnapshot::getRound).orElse(0);
    }

    /** Congela el ranking actual como snapshot (para los deltas de la próxima jornada). */
    public LeaderboardSnapshot takeSnapshot() {
        int round = currentRound() + 1;
        List<RankEntry> ranks = ranking().stream()
                .map(r -> new RankEntry(r.userId(), r.rank(), r.points()))
                .toList();
        return snapshots.save(new LeaderboardSnapshot(null, Instant.now(clock), round, ranks));
    }

    private Map<String, Integer> latestSnapshotRanks() {
        Optional<LeaderboardSnapshot> last = snapshots.findTopByOrderByTakenAtDesc();
        if (last.isEmpty() || last.get().getRanks() == null) return Map.of();
        return last.get().getRanks().stream()
                .collect(Collectors.toMap(RankEntry::userId, RankEntry::rank, (a, b) -> a));
    }
}
