package com.wirbi.mundial.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatReactionRequest(@NotBlank String emoji) {
}
