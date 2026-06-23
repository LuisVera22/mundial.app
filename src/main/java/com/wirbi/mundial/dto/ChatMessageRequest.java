package com.wirbi.mundial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank(message = "El mensaje no puede estar vacío.")
        @Size(max = 280, message = "Máximo 280 caracteres.")
        String text
) {
}
