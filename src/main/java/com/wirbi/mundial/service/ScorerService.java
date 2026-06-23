package com.wirbi.mundial.service;

import com.wirbi.mundial.dto.ScorerDto;
import com.wirbi.mundial.integration.FootballDataMapper;
import com.wirbi.mundial.integration.ProviderScorer;
import com.wirbi.mundial.model.Scorer;
import com.wirbi.mundial.repository.ScorerRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Tabla de goleo: persiste el snapshot del proveedor y lo sirve a la app. */
@Service
public class ScorerService {

    private final ScorerRepository scorers;

    public ScorerService(ScorerRepository scorers) {
        this.scorers = scorers;
    }

    /**
     * Reemplaza la tabla de goleo con lo que entrega el proveedor. Una lista
     * vacía (plan sin /scorers o caída) NO borra la última conocida, igual que
     * el sync de partidos: preferimos datos viejos a una tabla en blanco.
     */
    public void replaceAll(List<ProviderScorer> provider) {
        if (provider == null || provider.isEmpty()) return;
        List<Scorer> rows = new ArrayList<>(provider.size());
        int pos = 1;
        for (ProviderScorer p : provider) {
            if (p.playerProviderId() == null) continue; // sin id estable, se omite
            String code = FootballDataMapper.teamCodeFromTla(p.teamTla());
            String teamName = code == null ? null : FootballDataMapper.localizedName(code, code);
            rows.add(new Scorer(p.playerProviderId(), pos++, p.playerName(), code, teamName,
                    p.goals(), p.assists(), p.penalties(), p.playedMatches()));
        }
        scorers.deleteAll();
        scorers.saveAll(rows);
    }

    /** Tabla de goleo en orden de posición (líder primero). */
    public List<ScorerDto> list() {
        return scorers.findAllByOrderByPositionAsc().stream()
                .map(s -> new ScorerDto(s.getPosition(), s.getName(), s.getTeamCode(), s.getTeamName(),
                        s.getGoals(), s.getAssists(), s.getPenalties(), s.getPlayedMatches()))
                .toList();
    }
}
