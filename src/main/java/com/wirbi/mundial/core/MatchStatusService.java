package com.wirbi.mundial.core;

import com.wirbi.mundial.config.TournamentProperties;
import com.wirbi.mundial.model.Match;
import com.wirbi.mundial.model.MatchStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Estado de un partido derivado del tiempo (port de lock.ts). Garantiza
 * consistencia aunque la pestaña quede abierta: el estado se calcula, no se
 * almacena. Usa el {@link Clock} inyectado (fijable en tests).
 */
@Service
public class MatchStatusService {

    private final TournamentProperties props;
    private final Clock clock;

    public MatchStatusService(TournamentProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;
    }

    public long msUntilKickoff(Match match, Instant now) {
        return match.getKickoff().toEpochMilli() - now.toEpochMilli();
    }

    public MatchStatus statusOf(Match match, Instant now) {
        if (match.getResult() != null) return MatchStatus.FINISHED;
        long ms = msUntilKickoff(match, now);
        if (ms <= props.lockMinutes() * 60_000L) return MatchStatus.LOCKED;
        if (ms < props.urgentMinutes() * 60_000L) return MatchStatus.URGENT;
        return MatchStatus.OPEN;
    }

    public MatchStatus statusOf(Match match) {
        return statusOf(match, Instant.now(clock));
    }

    public boolean isEditable(MatchStatus status) {
        return status == MatchStatus.OPEN || status == MatchStatus.URGENT;
    }

    public boolean isEditable(Match match) {
        return isEditable(statusOf(match));
    }
}
