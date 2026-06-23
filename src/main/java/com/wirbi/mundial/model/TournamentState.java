package com.wirbi.mundial.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Estado dinámico del torneo (doc único, _id = "current"): los finales
 * (campeón/goleador) que el sync/admin va fijando. Se separa de la config
 * estática (fechas) que vive en application.yml.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("tournamentState")
public class TournamentState {
    public static final String SINGLETON_ID = "current";

    @Id
    private String id;
    private Finals finals;
}
