package com.wirbi.mundial.service;

import com.wirbi.mundial.integration.FootballDataMapper;
import com.wirbi.mundial.integration.FootballProvider;
import com.wirbi.mundial.integration.ProviderMatch;
import com.wirbi.mundial.integration.ProviderScorer;
import com.wirbi.mundial.model.Finals;
import com.wirbi.mundial.model.Match;
import com.wirbi.mundial.model.Player;
import com.wirbi.mundial.model.Score;
import com.wirbi.mundial.model.Stage;
import com.wirbi.mundial.model.Team;
import com.wirbi.mundial.repository.MatchRepository;
import com.wirbi.mundial.repository.PlayerRepository;
import com.wirbi.mundial.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Orquesta la sincronización con el proveedor (DIP: depende del puerto
 * {@link FootballProvider}). Una sola llamada a /matches cubre fixtures,
 * resultados y el campeón; los goleadores son best-effort.
 */
@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    /**
     * Tope de llamadas al detalle por-id por sync. El plan Free permite 10
     * req/min; el sync ya gasta el listado masivo + goleadores, así que dejamos
     * margen. Si hay más partidos por completar, se resuelven en los próximos
     * syncs (cada uno toma los que aún no tienen resultado).
     */
    private static final int MAX_DETAIL_CALLS = 8;

    private final FootballProvider provider;
    private final MatchRepository matches;
    private final TeamRepository teams;
    private final PlayerRepository players;
    private final TournamentStateService tournamentState;
    private final LeaderboardService leaderboard;
    private final ScorerService scorerService;

    public SyncService(FootballProvider provider, MatchRepository matches, TeamRepository teams,
                       PlayerRepository players, TournamentStateService tournamentState,
                       LeaderboardService leaderboard, ScorerService scorerService) {
        this.provider = provider;
        this.matches = matches;
        this.teams = teams;
        this.players = players;
        this.tournamentState = tournamentState;
        this.leaderboard = leaderboard;
        this.scorerService = scorerService;
    }

    public SyncResult sync() {
        if (!provider.isConfigured()) {
            return SyncResult.skipped("Proveedor sin token (FOOTBALL_DATA_TOKEN). Sync deshabilitado.");
        }
        List<ProviderMatch> pms = provider.fetchMatches();

        // Guard: una respuesta vacía del proveedor (outage/degradación) no debe
        // tocar la BD — sin esto, la poda de huérfanos borraría los 48 equipos.
        if (pms.isEmpty()) {
            log.warn("El proveedor devolvió 0 partidos; sync abortado sin cambios.");
            return SyncResult.skipped("El proveedor devolvió 0 partidos; no se modificó nada.");
        }

        // Diagnóstico: qué etapas devuelve realmente el proveedor (raw → conteo).
        // Si aquí solo aparece GROUP_STAGE, el dataset aún no trae eliminatorias.
        Map<String, Long> stageHistogram = pms.stream()
                .collect(Collectors.groupingBy(p -> String.valueOf(p.stage()), TreeMap::new, Collectors.counting()));
        log.info("Distribución de etapas (raw del proveedor): {}", stageHistogram);

        // Resultados ya guardados. El listado masivo del plan Free marca los
        // partidos como FINISHED pero entrega el marcador en null (de forma
        // intermitente), así que NUNCA pisamos un resultado existente con null:
        // una vez capturado, queda fijado aunque el proveedor lo vuelva a omitir.
        Map<String, Score> existingResults = matches.findAll().stream()
                .filter(m -> m.getResult() != null)
                .collect(Collectors.toMap(Match::getId, Match::getResult));

        Set<String> seenCodes = new HashSet<>();
        String championCode = null;
        int detailCalls = 0;
        boolean newResults = false;
        // Acumulamos los partidos a guardar: la persistencia se difiere hasta
        // después del snapshot, para que la foto del ranking sea la de ANTES.
        List<Match> toSave = new ArrayList<>();

        for (ProviderMatch raw : pms) {
            // Pedimos el detalle por-id (único endpoint del plan Free que expone
            // el marcador) cuando: (a) terminó pero el listado masivo no trae el
            // marcador y aún no lo tenemos, o (b) está EN CURSO (para el marcador
            // en vivo, que cambia cada sync). Con tope por sync para no pasar el
            // límite de 10 req/min del proveedor.
            ProviderMatch pm = raw;
            boolean needFinalScore = raw.finished() && raw.homeScore() == null
                    && !existingResults.containsKey(raw.providerId());
            if ((needFinalScore || raw.live()) && detailCalls < MAX_DETAIL_CALLS) {
                detailCalls++;
                ProviderMatch detail = provider.fetchMatch(raw.providerId());
                if (detail != null) pm = detail;
            }

            String homeCode = FootballDataMapper.teamCode(pm.home());
            String awayCode = FootballDataMapper.teamCode(pm.away());
            String group = FootballDataMapper.groupLetter(pm.group());
            Stage stage = FootballDataMapper.toStage(pm.stage());

            upsertTeam(homeCode, pm.home() != null ? pm.home().name() : null, group,
                    pm.home() != null ? pm.home().providerId() : null);
            upsertTeam(awayCode, pm.away() != null ? pm.away().name() : null, group,
                    pm.away() != null ? pm.away().providerId() : null);
            if (homeCode != null) seenCodes.add(homeCode);
            if (awayCode != null) seenCodes.add(awayCode);

            // Marcador FINAL del proveedor si lo trae; si no, conservar el guardado.
            // (Solo FINISHED → este es el que puntúa el ranking.)
            Score result = (pm.finished() && pm.homeScore() != null && pm.awayScore() != null)
                    ? new Score(pm.homeScore(), pm.awayScore())
                    : existingResults.get(pm.providerId());

            // ¿Resultado nuevo? (un partido que no tenía marcador y ahora sí).
            if (result != null && !existingResults.containsKey(pm.providerId())) {
                newResults = true;
            }

            // Marcador EN VIVO (solo display): se fija mientras el partido está
            // en curso; al finalizar queda null y manda `result`. NUNCA puntúa.
            Score liveScore = (pm.live() && pm.homeScore() != null && pm.awayScore() != null)
                    ? new Score(pm.homeScore(), pm.awayScore())
                    : null;

            Match m = new Match(pm.providerId(), homeCode, awayCode, pm.kickoff(),
                    stage, group, pm.venue(), result, pm.providerId());
            m.setLiveScore(liveScore);
            m.setLiveStatus(pm.live() ? pm.status() : null);
            toSave.add(m);

            // Campeón = ganador del partido FINAL (incluye penales vía 'winner')
            if (stage == Stage.FINAL && pm.finished()) {
                if ("HOME_TEAM".equals(pm.winner())) championCode = homeCode;
                else if ("AWAY_TEAM".equals(pm.winner())) championCode = awayCode;
            }
        }

        // Antes de aplicar resultados nuevos, congelamos la foto del ranking
        // para que los deltas ▲/▼ reflejen el movimiento que causarán. Si no
        // entra ningún resultado nuevo, no se toma snapshot (evita "fotos"
        // idénticas que reiniciarían los deltas a cero).
        if (newResults) {
            leaderboard.takeSnapshot();
        }
        matches.saveAll(toSave);

        // Poda de huérfanos: el sync es la fuente de verdad de teams. Elimina los
        // que ya no produce esta sincronización (p. ej. códigos viejos tras un
        // cambio de mapeo) → evita duplicados en la vista de grupos.
        List<Team> stale = teams.findAll().stream()
                .filter(t -> !seenCodes.contains(t.getCode()))
                .toList();
        if (!stale.isEmpty()) {
            teams.deleteAll(stale);
            log.info("Poda de {} equipos huérfanos: {}", stale.size(),
                    stale.stream().map(Team::getCode).toList());
        }

        // Goleadores: una sola llamada para la tabla de goleo, que es SOLO
        // informativa (muestra al líder en vivo). Si su persistencia fallara, NO
        // debe abortar el sync (resultados/ranking ya quedaron guardados arriba y
        // son lo crítico).
        List<ProviderScorer> scorers = provider.fetchScorers();
        try {
            scorerService.replaceAll(scorers);
        } catch (RuntimeException ex) {
            log.warn("No se pudo actualizar la tabla de goleo ({}); el sync continúa.", ex.getMessage());
        }
        boolean scorersAvailable = !scorers.isEmpty();

        // Finales (sobre esto puntúan los picks globales). Campeón y Bota de Oro
        // se resuelven SOLO cuando el torneo terminó: su valor definitivo no existe
        // antes de la FINAL. El campeón es el ganador de la FINAL; el goleador, el
        // líder de goleo una vez jugada. Durante el torneo la tabla de goleo es solo
        // informativa y NO puntúa, para no premiar a un líder provisional.
        Finals current = tournamentState.finals();
        String champion = championCode != null ? championCode : current.champion();
        boolean tournamentOver = champion != null;
        String scorerId = null;
        if (tournamentOver) {
            String resolved = resolveTopScorerId(scorers);
            scorerId = resolved != null ? resolved : current.scorer();
        }
        Finals merged = new Finals(champion, scorerId);
        tournamentState.setFinals(merged);

        log.info("Sync OK: {} partidos, {} equipos, campeón={}, goleador={}",
                pms.size(), seenCodes.size(), merged.champion(), merged.scorer());
        return new SyncResult(false, "Sincronización completada.", pms.size(), seenCodes.size(),
                merged.champion(), merged.scorer(), scorersAvailable);
    }

    private int upsertTeam(String code, String name, String group, String providerId) {
        if (code == null) return 0;
        Team t = teams.findById(code).orElseGet(Team::new);
        t.setCode(code);
        // Nombre en español (fallback al del proveedor en inglés si no está mapeado).
        t.setName(FootballDataMapper.localizedName(code, name != null ? name : code));
        if (group != null) t.setGroup(group);
        if (providerId != null) t.setProviderId(providerId);
        teams.save(t);
        return 1;
    }

    /**
     * Goleador del proveedor → nuestro player.id (best-effort). Solo se usa para
     * fijar la Bota de Oro al TERMINAR el torneo (ver {@link #sync()}): entonces
     * el líder de goleo ya es definitivo. El proveedor entrega la lista rankeada;
     * {@code max(goles)} conserva al primero en empates, respetando su desempate
     * oficial (que incluye minutos jugados, dato que nosotros no tenemos). El
     * emparejamiento es en capas:
     *  1) por {@code providerId} (id estable de football-data.org), y
     *  2) si no, por nombre normalizado (tolera acentos y mayúsculas).
     * Si no casa con nadie, devuelve null y se conserva el goleador manual.
     */
    private String resolveTopScorerId(List<ProviderScorer> scorers) {
        if (scorers.isEmpty()) return null;
        ProviderScorer top = scorers.stream().max(Comparator.comparingInt(ProviderScorer::goals)).orElse(null);
        if (top == null) return null;

        List<Player> all = players.findAll();
        // 1) Match por providerId (estable, sin acentos).
        if (top.playerProviderId() != null) {
            String byId = all.stream()
                    .filter(p -> top.playerProviderId().equals(p.getProviderId()))
                    .map(Player::getId)
                    .findFirst()
                    .orElse(null);
            if (byId != null) return byId;
        }
        // 2) Fallback por nombre normalizado.
        if (top.playerName() != null) {
            String key = FootballDataMapper.normalizeName(top.playerName());
            String byName = all.stream()
                    .filter(p -> FootballDataMapper.normalizeName(p.getName()).equals(key))
                    .map(Player::getId)
                    .findFirst()
                    .orElse(null);
            if (byName != null) return byName;
        }
        // Condición normal: el líder de goleo puede no estar entre los candidatos
        // curados de la Bota de Oro (entonces nadie lo eligió). debug, no warn:
        // se repetiría en cada sync y no es accionable.
        log.debug("El goleador líder '{}' (id={}) no está entre los candidatos locales a la "
                + "Bota de Oro; se conserva el goleador manual.", top.playerName(), top.playerProviderId());
        return null;
    }
}
