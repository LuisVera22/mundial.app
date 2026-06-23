package com.wirbi.mundial.integration;

/** Equipo tal como lo entrega el proveedor (normalizado). Campos null = TBD. */
public record ProviderTeam(String providerId, String name, String tla) {
}
