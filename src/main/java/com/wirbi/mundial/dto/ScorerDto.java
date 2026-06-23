package com.wirbi.mundial.dto;

/** Fila de la tabla de goleo para el cliente. */
public record ScorerDto(int position, String name, String teamCode, String teamName,
                        int goals, Integer assists, Integer penalties, Integer playedMatches) {
}
