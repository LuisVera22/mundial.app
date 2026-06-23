package com.wirbi.mundial.model;

/** Estado de un partido derivado del tiempo (port de lock.ts). */
public enum MatchStatus {
    /** Editable con holgura. */
    OPEN,
    /** &lt; 60 min al pitazo, aún editable. */
    URGENT,
    /** &le; 15 min al pitazo (o ya empezó): edición cerrada. */
    LOCKED,
    /** Hay resultado real. */
    FINISHED
}
