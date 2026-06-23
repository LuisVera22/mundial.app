package com.wirbi.mundial.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Colaborador. _id = id de dev (stub) ahora; será el {@code oid} de Entra al
 * implementar auth. Los picks globales se embeben aquí.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("users")
public class User {
    @Id
    private String id;
    private String name;
    private int hue;            // 0-360 para el gradiente del avatar
    private String avatar;      // archivo elegido (null = iniciales con hue)
    private boolean onboardingDone;
    private GlobalPicks globalPicks;
    private boolean betaAccess; // piloto: habilita features en beta (la Tribuna)
}
