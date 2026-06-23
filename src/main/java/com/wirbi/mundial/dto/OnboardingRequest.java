package com.wirbi.mundial.dto;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo del onboarding: campeón + goleador (ambos requeridos). */
public record OnboardingRequest(
        @NotBlank String champion,
        @NotBlank String scorer
) {
}
