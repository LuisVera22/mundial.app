package com.wirbi.mundial.dto;

/**
 * Actualización parcial de picks globales. Un campo null = "no tocar"; solo se
 * re-estampa el savedAt del campo cuyo valor cambia.
 */
public record GlobalPicksRequest(String champion, String scorer) {
}
