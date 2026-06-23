package com.wirbi.mundial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provee un {@link Clock} inyectable. Toda la lógica temporal (lock de partidos,
 * devaluación de picks) lo usa en vez de Instant.now(), para poder fijar el
 * tiempo en los tests (equivalente al parámetro `now` que pasaba el front TS).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
