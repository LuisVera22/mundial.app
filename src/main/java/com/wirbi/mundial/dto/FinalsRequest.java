package com.wirbi.mundial.dto;

/** Fijar manualmente los finales del torneo (campeón / goleador). */
public record FinalsRequest(String champion, String scorer) {
}
