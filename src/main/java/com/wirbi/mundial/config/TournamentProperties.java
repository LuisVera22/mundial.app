package com.wirbi.mundial.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Instant;

/**
 * Configuración estática del torneo (valores fijos, en application.yml).
 *  - startAt → inicio del torneo (fase de grupos).
 *  - roundOf32At → inicio de dieciseisavos · roundOf16At → inicio de octavos.
 *    Definen los ESCALONES de valor de los picks globales (campeón/goleador):
 *    el valor que tiene una apuesta depende de la fase en que se confirma.
 *  - picksDeadlineAt → fin de octavos: tras esta fecha los picks se bloquean.
 *  - lockMinutes / urgentMinutes: bloqueo y urgencia de partidos.
 *
 * El estado DINÁMICO (campeón/goleador finales) vive en Mongo
 * (TournamentState), no aquí.
 */
@ConfigurationProperties(prefix = "tournament")
public record TournamentProperties(
        Instant startAt,
        Instant roundOf32At,
        Instant roundOf16At,
        Instant picksDeadlineAt,
        int lockMinutes,
        int urgentMinutes
) {
}
