package com.wirbi.mundial.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wirbi.mundial.config.FootballDataProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

/**
 * Adapter de football-data.org (plan Free) sobre {@link FootballProvider}.
 * Usa un solo endpoint para casi todo: GET /competitions/{WC}/matches.
 * Auth por header X-Auth-Token. Maneja 429 y la ausencia de /scorers en Free.
 */
@Component
public class FootballDataOrgProvider implements FootballProvider {

    private static final Logger log = LoggerFactory.getLogger(FootballDataOrgProvider.class);

    private final FootballDataProperties props;
    private final RestClient client;

    public FootballDataOrgProvider(FootballDataProperties props) {
        this.props = props;
        this.client = RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("X-Auth-Token", props.apiToken() == null ? "" : props.apiToken())
                .build();
    }

    @Override
    public boolean isConfigured() {
        return props.configured();
    }

    @Override
    public List<ProviderMatch> fetchMatches() {
        MatchesResponse resp = client.get()
                .uri("/competitions/{c}/matches", props.competition())
                .retrieve()
                .onStatus(s -> s.value() == 429, (req, res) -> {
                    throw new IllegalStateException("football-data.org: rate limit (429) excedido");
                })
                .body(MatchesResponse.class);
        if (resp == null || resp.matches() == null) return List.of();
        return resp.matches().stream().map(FootballDataOrgProvider::toProviderMatch).toList();
    }

    @Override
    public ProviderMatch fetchMatch(String providerId) {
        try {
            RawMatch m = client.get()
                    .uri("/matches/{id}", providerId)
                    .retrieve()
                    .onStatus(s -> s.value() == 429, (req, res) -> {
                        throw new IllegalStateException("football-data.org: rate limit (429) excedido");
                    })
                    .body(RawMatch.class);
            return m == null ? null : toProviderMatch(m);
        } catch (RuntimeException ex) {
            log.warn("No se pudo obtener el detalle del partido {} ({}).", providerId, ex.getMessage());
            return null;
        }
    }

    @Override
    public List<ProviderScorer> fetchScorers() {
        try {
            ScorersResponse resp = client.get()
                    .uri("/competitions/{c}/scorers", props.competition())
                    .retrieve()
                    .body(ScorersResponse.class);
            if (resp == null || resp.scorers() == null) return List.of();
            return resp.scorers().stream()
                    .filter(s -> s.player() != null)
                    .map(s -> new ProviderScorer(
                            s.player().id() == null ? null : String.valueOf(s.player().id()),
                            s.player().name(),
                            s.team() == null ? null : s.team().tla(),
                            s.goals() == null ? 0 : s.goals(),
                            s.assists(),
                            s.penalties(),
                            s.playedMatches()))
                    .toList();
        } catch (RuntimeException ex) {
            // /scorers puede no estar en el plan Free (403) → degradar a vacío
            log.warn("No se pudieron obtener goleadores ({}). Se usará el set manual de finales.",
                    ex.getMessage());
            return List.of();
        }
    }

    private static ProviderMatch toProviderMatch(RawMatch m) {
        Integer hs = m.score() != null && m.score().fullTime() != null ? m.score().fullTime().home() : null;
        Integer as = m.score() != null && m.score().fullTime() != null ? m.score().fullTime().away() : null;
        String winner = m.score() != null ? m.score().winner() : null;
        return new ProviderMatch(
                String.valueOf(m.id()),
                m.utcDate() != null ? Instant.parse(m.utcDate()) : null,
                m.status(),
                m.stage(),
                m.group(),
                toTeam(m.homeTeam()),
                toTeam(m.awayTeam()),
                hs, as, winner, m.venue());
    }

    private static ProviderTeam toTeam(RawTeam t) {
        if (t == null) return null;
        return new ProviderTeam(t.id() == null ? null : String.valueOf(t.id()), t.name(), t.tla());
    }

    // ---- Binding del JSON crudo de football-data.org (solo lo que usamos) ----
    @JsonIgnoreProperties(ignoreUnknown = true)
    record MatchesResponse(List<RawMatch> matches) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawMatch(long id, String utcDate, String status, String stage, String group,
                    RawTeam homeTeam, RawTeam awayTeam, RawScore score, String venue) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawTeam(Long id, String name, String tla) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawScore(String winner, RawFullTime fullTime) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawFullTime(Integer home, Integer away) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ScorersResponse(List<RawScorer> scorers) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawScorer(RawPlayer player, RawTeam team, Integer goals, Integer assists,
                     Integer penalties, Integer playedMatches) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawPlayer(Long id, String name) {
    }
}
