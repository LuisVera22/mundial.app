package com.wirbi.mundial.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del proveedor de datos football-data.org (plan Free).
 * `apiToken` vacío → el sync queda deshabilitado (no-op).
 */
@ConfigurationProperties(prefix = "football-data")
public record FootballDataProperties(
        String baseUrl,
        String competition,
        String apiToken
) {
    public boolean configured() {
        return apiToken != null && !apiToken.isBlank();
    }
}
