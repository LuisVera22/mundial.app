package com.wirbi.mundial.dto;

/** Reacciones agregadas por emoji; {@code mine} = el usuario actual reaccionó. */
public record ChatReactionDto(String emoji, int count, boolean mine) {
}
