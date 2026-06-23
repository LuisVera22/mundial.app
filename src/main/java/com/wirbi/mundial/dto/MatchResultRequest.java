package com.wirbi.mundial.dto;

/**
 * Marcador final fijado a mano (respaldo cuando el proveedor no expone el
 * resultado). Ambos null → limpia el resultado del partido.
 */
public record MatchResultRequest(Integer home, Integer away) {
}
