package com.wirbi.mundial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cambio de avatar: nombre de archivo del catálogo del front. */
public record AvatarRequest(
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "^[a-zA-Z0-9_-]+\\.png$", message = "avatar inválido")
        String avatar
) {
}
