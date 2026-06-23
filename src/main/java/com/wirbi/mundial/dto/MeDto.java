package com.wirbi.mundial.dto;

/**
 * Perfil del usuario actual. points/rank/stats se completan cuando esté el
 * leaderboard (pueden venir null hasta entonces).
 */
public record MeDto(
        String id,
        String name,
        int hue,
        String avatar,
        boolean onboardingDone,
        Integer points,
        Integer rank,
        Integer exactCount,
        Integer trendCount,
        Integer matchPoints,
        GlobalPicksDto globalPicks,
        boolean betaAccess
) {
}
