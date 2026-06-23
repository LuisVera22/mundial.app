package com.wirbi.mundial.integration;

import java.util.List;

/**
 * Puerto al proveedor de datos de fútbol (DIP/OCP): el dominio depende de esta
 * abstracción, no de football-data.org. Para agregar otro proveedor
 * (p. ej. API-Football) basta una nueva implementación, sin tocar SyncService.
 */
public interface FootballProvider {

    /** ¿Hay credenciales configuradas? Si no, el sync es no-op. */
    boolean isConfigured();

    /** Todos los partidos del torneo (fixtures + estado + marcadores) en 1 llamada. */
    List<ProviderMatch> fetchMatches();

    /**
     * Detalle de un partido por id. El listado masivo del plan Free marca los
     * partidos como FINISHED pero omite el marcador; el detalle individual sí
     * lo expone (de forma intermitente). Devuelve null si no está disponible.
     */
    ProviderMatch fetchMatch(String providerId);

    /** Goleadores del torneo (puede no estar disponible en el plan Free → lista vacía). */
    List<ProviderScorer> fetchScorers();
}
