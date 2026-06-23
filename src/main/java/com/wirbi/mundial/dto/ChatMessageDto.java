package com.wirbi.mundial.dto;

import java.time.Instant;
import java.util.List;

/** Mensaje de la Tribuna listo para la UI; {@code mine} marca los propios. */
public record ChatMessageDto(
        String id,
        String name,
        int hue,
        String avatar,
        Integer rank,
        Integer points,
        String text,
        Instant createdAt,
        boolean mine,
        List<ChatReactionDto> reactions
) {
}
