package com.wirbi.mundial.dto;

/** Fila del ranking. delta &gt; 0 = subió ▲, &lt; 0 = bajó ▼, 0 = sin cambio. */
public record LeaderboardRowDto(
        int rank,
        String userId,
        String name,
        int hue,
        String avatar,
        int points,
        int delta,
        boolean me
) {
}
