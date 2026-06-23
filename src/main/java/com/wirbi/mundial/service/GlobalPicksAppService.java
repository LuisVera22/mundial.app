package com.wirbi.mundial.service;

import com.wirbi.mundial.model.GlobalPicks;
import com.wirbi.mundial.model.User;
import com.wirbi.mundial.repository.UserRepository;
import com.wirbi.mundial.core.GlobalPicksService;
import com.wirbi.mundial.core.ScoringService;
import com.wirbi.mundial.exception.ConflictException;
import com.wirbi.mundial.dto.GlobalPicksDto;
import com.wirbi.mundial.dto.PlayerDto;
import com.wirbi.mundial.dto.TeamRef;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Aplicación de picks globales: lectura con valores devaluados, actualización
 * parcial (re-estampa savedAt solo del campo cambiado) con enforcement del
 * deadline, y onboarding.
 */
@Service
public class GlobalPicksAppService {

    private final UserRepository users;
    private final GlobalPicksService rules;
    private final ReferenceService reference;
    private final Clock clock;

    public GlobalPicksAppService(UserRepository users, GlobalPicksService rules,
                                 ReferenceService reference, Clock clock) {
        this.users = users;
        this.rules = rules;
        this.reference = reference;
        this.clock = clock;
    }

    public GlobalPicksDto get(User user) {
        return toDto(currentPicks(user));
    }

    /** Actualiza parcialmente; re-estampa savedAt solo del campo cuyo valor cambia. */
    public GlobalPicksDto update(User user, String champion, String scorer) {
        if (rules.isLocked()) {
            throw new ConflictException("La edición de predicciones globales está cerrada (fin de octavos).");
        }
        GlobalPicks cur = currentPicks(user);
        Instant now = Instant.now(clock);

        String newChampion = cur.champion();
        Instant championSavedAt = cur.championSavedAt();
        if (champion != null && !Objects.equals(champion, cur.champion())) {
            newChampion = champion;
            championSavedAt = now;
        }

        String newScorer = cur.scorer();
        Instant scorerSavedAt = cur.scorerSavedAt();
        if (scorer != null && !Objects.equals(scorer, cur.scorer())) {
            newScorer = scorer;
            scorerSavedAt = now;
        }

        GlobalPicks updated = new GlobalPicks(newChampion, newScorer, championSavedAt, scorerSavedAt);
        user.setGlobalPicks(updated);
        users.save(user);
        return toDto(updated);
    }

    public GlobalPicksDto onboarding(User user, String champion, String scorer) {
        Instant now = Instant.now(clock);
        user.setGlobalPicks(new GlobalPicks(champion, scorer, now, now));
        user.setOnboardingDone(true);
        users.save(user);
        return toDto(user.getGlobalPicks());
    }

    private GlobalPicks currentPicks(User user) {
        return user.getGlobalPicks() != null ? user.getGlobalPicks() : GlobalPicks.empty();
    }

    private GlobalPicksDto toDto(GlobalPicks gp) {
        TeamRef champion = gp.champion() != null ? reference.teamRef(gp.champion()) : null;
        Integer championValue = gp.champion() != null ? rules.championValue(gp.championSavedAt()) : null;
        PlayerDto scorer = gp.scorer() != null ? reference.playerDto(gp.scorer()) : null;
        Integer scorerValue = gp.scorer() != null ? rules.scorerValue(gp.scorerSavedAt()) : null;
        return new GlobalPicksDto(
                champion, championValue, scorer, scorerValue,
                ScoringService.CHAMPION, rules.championFloorValue(),
                ScoringService.SCORER, rules.scorerFloorValue(),
                rules.isLocked(), rules.deadline());
    }
}
