package com.wirbi.mundial.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Selección participante. _id = código (ej. "ar", "br", "gb-eng"). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("teams")
public class Team {
    @Id
    private String code;
    private String name;
    private String group;       // A-H (o null en fases finales)
    private String providerId;  // id en football-data.org (mapeo)
}
