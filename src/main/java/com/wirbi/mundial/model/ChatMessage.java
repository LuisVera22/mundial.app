package com.wirbi.mundial.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Mensaje de la Tribuna (sala global, piloto beta). Efímero por diseño: el
 * índice TTL borra cada mensaje 24 h después de creado — la conversación de
 * hoy es sobre los partidos de hoy, y no hay historial que moderar.
 * rank/points se congelan al momento de escribir (presumes el puesto que
 * tenías, no el que tendrás).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("chatMessages")
public class ChatMessage {
    @Id
    private String id;
    private String userId;
    private String text;
    private Integer rank;        // puesto en el ranking al escribir (null si aún sin ranking)
    private Integer points;      // puntos al escribir
    private List<ChatReaction> reactions;

    @Indexed(expireAfter = "24h")
    private Instant createdAt;
}
