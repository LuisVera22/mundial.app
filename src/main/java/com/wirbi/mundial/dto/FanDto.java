package com.wirbi.mundial.dto;

/**
 * Hincha de una selección: usuario cuyo pick global de campeón es ese país.
 * Solo datos públicos de presentación (los mismos que muestra el ranking).
 */
public record FanDto(String name, int hue, String avatar) {
}
