package com.wirbi.mundial.model;

/** Entrada de un snapshot de ranking (para calcular deltas ▲/▼). */
public record RankEntry(String userId, int rank, int points) {
}
