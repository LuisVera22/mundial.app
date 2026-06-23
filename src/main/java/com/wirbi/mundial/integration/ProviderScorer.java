package com.wirbi.mundial.integration;

/**
 * Goleador normalizado desde el proveedor. {@code assists}, {@code penalties} y
 * {@code playedMatches} pueden venir null (no siempre los expone el proveedor).
 */
public record ProviderScorer(String playerProviderId, String playerName, String teamTla,
                             int goals, Integer assists, Integer penalties, Integer playedMatches) {
}
