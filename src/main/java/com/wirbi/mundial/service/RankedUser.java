package com.wirbi.mundial.service;

/**
 * Usuario con su puntuación y posición ya calculadas. Reutilizado por el
 * leaderboard y por el perfil (/me).
 */
public record RankedUser(
        String userId,
        String name,
        int hue,
        String avatar,
        int rank,
        int points,
        int matchPoints,
        int globalPoints,
        int exactCount,
        int trendCount,
        int delta
) {
}
