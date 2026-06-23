package com.wirbi.mundial.dto;

import java.time.Instant;

public record PredictionDto(String matchId, int home, int away, Instant updatedAt) {
}
