package com.wirbi.mundial.model;

/** Fase del torneo. La etiqueta es la usada por el front. */
public enum Stage {
    GROUP("Fase de Grupos"),
    ROUND_OF_32("Dieciseisavos"),
    ROUND_OF_16("Octavos"),
    QUARTER_FINAL("Cuartos"),
    SEMI_FINAL("Semifinal"),
    THIRD_PLACE("Tercer puesto"),
    FINAL("Final");

    private final String label;

    Stage(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
