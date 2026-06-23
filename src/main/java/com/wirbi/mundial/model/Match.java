package com.wirbi.mundial.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Partido. Las eliminatorias pueden tener {@code home}/{@code away} nulos (TBD)
 * hasta que se definen.
 *
 * <p>Dos marcadores, deliberadamente separados:
 * <ul>
 *   <li>{@code result}: marcador FINAL (null hasta que finaliza). Es el ÚNICO
 *       que alimenta el ranking — nunca se toca con datos en vivo.</li>
 *   <li>{@code liveScore}/{@code liveStatus}: marcador en curso, solo para
 *       mostrar (IN_PLAY/PAUSED). No interviene en la puntuación.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@Document("matches")
public class Match {
    @Id
    private String id;
    private String home;        // Team.code (null si TBD)
    private String away;        // Team.code (null si TBD)
    @Indexed
    private Instant kickoff;
    private Stage stage;
    private String group;       // A-H (solo fase de grupos)
    private String venue;
    private Score result;       // marcador final; null hasta que finaliza
    private String providerId;  // id del partido en football-data.org
    private Score liveScore;    // marcador en curso (display); null si no aplica
    private String liveStatus;  // estado del proveedor en vivo: IN_PLAY / PAUSED

    /** Constructor base (sin datos en vivo); los campos live se fijan por setter. */
    public Match(String id, String home, String away, Instant kickoff, Stage stage,
                 String group, String venue, Score result, String providerId) {
        this.id = id;
        this.home = home;
        this.away = away;
        this.kickoff = kickoff;
        this.stage = stage;
        this.group = group;
        this.venue = venue;
        this.result = result;
        this.providerId = providerId;
    }
}
