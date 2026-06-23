package com.wirbi.mundial.service;

import com.wirbi.mundial.model.Match;
import com.wirbi.mundial.model.Prediction;
import com.wirbi.mundial.repository.MatchRepository;
import com.wirbi.mundial.repository.PredictionRepository;
import com.wirbi.mundial.core.MatchStatusService;
import com.wirbi.mundial.exception.ConflictException;
import com.wirbi.mundial.exception.NotFoundException;
import com.wirbi.mundial.dto.PredictionDto;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** Pronósticos de marcador del usuario, con enforcement de bloqueo en servidor. */
@Service
public class PredictionService {

    private final PredictionRepository predictions;
    private final MatchRepository matches;
    private final MatchStatusService statusService;
    private final Clock clock;

    public PredictionService(PredictionRepository predictions, MatchRepository matches,
                             MatchStatusService statusService, Clock clock) {
        this.predictions = predictions;
        this.matches = matches;
        this.statusService = statusService;
        this.clock = clock;
    }

    public List<PredictionDto> mine(String userId) {
        return predictions.findByUserId(userId).stream()
                .map(p -> new PredictionDto(p.getMatchId(), p.getHome(), p.getAway(), p.getUpdatedAt()))
                .toList();
    }

    /** Crea o actualiza el pronóstico; rechaza si el partido ya no es editable. */
    public PredictionDto upsert(String userId, String matchId, int home, int away) {
        Match match = matches.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Partido no encontrado: " + matchId));
        if (!statusService.isEditable(match)) {
            throw new ConflictException("El pronóstico está cerrado para este partido.");
        }
        Prediction p = predictions.findByUserIdAndMatchId(userId, matchId)
                .orElseGet(() -> {
                    Prediction np = new Prediction();
                    np.setUserId(userId);
                    np.setMatchId(matchId);
                    return np;
                });
        p.setHome(home);
        p.setAway(away);
        p.setUpdatedAt(Instant.now(clock));
        predictions.save(p);
        return new PredictionDto(p.getMatchId(), p.getHome(), p.getAway(), p.getUpdatedAt());
    }
}
