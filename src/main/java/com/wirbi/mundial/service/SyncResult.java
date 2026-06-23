package com.wirbi.mundial.service;

/** Resumen de una sincronización con el proveedor. */
public record SyncResult(
        boolean skipped,
        String message,
        int matchesSynced,
        int teamsSynced,
        String champion,
        String scorer,
        boolean scorersAvailable
) {
    public static SyncResult skipped(String message) {
        return new SyncResult(true, message, 0, 0, null, null, false);
    }
}
