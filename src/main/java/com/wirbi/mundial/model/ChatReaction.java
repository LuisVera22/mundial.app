package com.wirbi.mundial.model;

/** Reacción de un usuario a un mensaje de la Tribuna (una por user+emoji). */
public record ChatReaction(String userId, String emoji) {
}
