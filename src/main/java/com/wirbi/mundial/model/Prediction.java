package com.wirbi.mundial.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Pronóstico de marcador de un usuario para un partido (un doc por user+match). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("predictions")
@CompoundIndex(name = "user_match_unique", def = "{'userId': 1, 'matchId': 1}", unique = true)
public class Prediction {
    @Id
    private String id;
    private String userId;
    private String matchId;
    private int home;
    private int away;
    private Instant updatedAt;
}
