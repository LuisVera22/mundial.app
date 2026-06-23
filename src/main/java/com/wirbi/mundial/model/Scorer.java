package com.wirbi.mundial.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Fila de la tabla de goleo del torneo (Bota de Oro). Snapshot del proveedor que
 * el sync reemplaza por completo en cada corrida; es solo lectura para la app.
 * {@code _id} = id del jugador en football-data.org. {@code position} preserva el
 * ranking del proveedor (que ya aplica el desempate oficial, incluidos minutos).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("scorers")
public class Scorer {
    @Id
    private String id;
    private int position;
    private String name;
    private String teamCode;   // ISO alpha-2 para la bandera (null si no mapea)
    private String teamName;   // nombre localizado de la selección
    private int goals;
    private Integer assists;
    private Integer penalties;
    private Integer playedMatches;
}
