package com.wirbi.mundial.dto;

/** Referencia mínima a un equipo (code + nombre). Null-safe para partidos TBD. */
public record TeamRef(String code, String name) {
}
