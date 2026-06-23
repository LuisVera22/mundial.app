package com.wirbi.mundial.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Jugador candidato a goleador (Bota de Oro). _id = ej. "p-vinicius". */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("players")
public class Player {
    @Id
    private String id;
    private String name;
    private String teamCode;
    private String providerId;  // id en football-data.org (mapeo)
    private Integer rank;       // posición en el ranking de favoritos a la Bota de Oro (1 = más favorito)
}
