package com.wirbi.mundial.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/** Snapshot del ranking en una jornada, para calcular el delta ▲/▼. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("leaderboardSnapshots")
public class LeaderboardSnapshot {
    @Id
    private String id;
    @Indexed
    private Instant takenAt;
    private int round;
    private List<RankEntry> ranks;
}
