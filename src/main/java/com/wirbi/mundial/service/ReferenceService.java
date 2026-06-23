package com.wirbi.mundial.service;

import com.wirbi.mundial.model.Player;
import com.wirbi.mundial.model.Team;
import com.wirbi.mundial.model.User;
import com.wirbi.mundial.repository.PlayerRepository;
import com.wirbi.mundial.repository.TeamRepository;
import com.wirbi.mundial.repository.UserRepository;
import com.wirbi.mundial.dto.FanDto;
import com.wirbi.mundial.dto.GroupDto;
import com.wirbi.mundial.dto.PlayerDto;
import com.wirbi.mundial.dto.TeamDto;
import com.wirbi.mundial.dto.TeamRef;
import com.wirbi.mundial.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Datos de referencia (equipos/jugadores) y helpers de mapeo a DTOs. */
@Service
public class ReferenceService {

    private final TeamRepository teams;
    private final PlayerRepository players;
    private final UserRepository users;

    public ReferenceService(TeamRepository teams, PlayerRepository players, UserRepository users) {
        this.teams = teams;
        this.players = players;
        this.users = users;
    }

    public TeamRef teamRef(String code) {
        if (code == null) return null;
        return teams.findById(code)
                .map(t -> new TeamRef(t.getCode(), t.getName()))
                .orElse(new TeamRef(code, code));
    }

    public PlayerDto playerDto(String id) {
        if (id == null) return null;
        Player p = players.findById(id).orElse(null);
        if (p == null) return null;
        String teamName = teams.findById(p.getTeamCode()).map(Team::getName).orElse(p.getTeamCode());
        return new PlayerDto(p.getId(), p.getName(), p.getTeamCode(), teamName, p.getRank());
    }

    public List<TeamDto> allTeams() {
        // Lista liviana (pickers): sin hinchas para no inflar la respuesta.
        return teams.findAll().stream()
                .sorted(Comparator.comparing(Team::getName))
                .map(t -> new TeamDto(t.getCode(), t.getName(), t.getGroup(), List.of()))
                .toList();
    }

    /**
     * Hinchas por selección: usuarios cuyo pick global de campeón es ese país,
     * en orden alfabético. Alimenta la píldora social de las tarjetas de país.
     */
    private Map<String, List<FanDto>> fansByTeam() {
        return users.findAll().stream()
                .filter(u -> u.getGlobalPicks() != null && u.getGlobalPicks().champion() != null)
                .sorted(Comparator.comparing(User::getName))
                .collect(Collectors.groupingBy(u -> u.getGlobalPicks().champion(),
                        Collectors.mapping(u -> new FanDto(u.getName(), u.getHue(), u.getAvatar()),
                                Collectors.toList())));
    }

    public List<PlayerDto> allPlayers() {
        return players.findAll().stream()
                .map(p -> new PlayerDto(p.getId(), p.getName(), p.getTeamCode(),
                        teams.findById(p.getTeamCode()).map(Team::getName).orElse(p.getTeamCode()),
                        p.getRank()))
                // Orden por ranking de favoritos (1 primero); sin rank al final, luego por nombre.
                .sorted(Comparator.comparing(PlayerDto::rank, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PlayerDto::name))
                .toList();
    }

    /** Selecciones agrupadas por grupo (A→L), omitiendo las sin grupo asignado. */
    public List<GroupDto> groups() {
        Map<String, List<FanDto>> fans = fansByTeam();
        Map<String, List<TeamDto>> byGroup = teams.findAll().stream()
                .filter(t -> t.getGroup() != null && !t.getGroup().isBlank())
                .sorted(Comparator.comparing(Team::getName))
                .collect(Collectors.groupingBy(Team::getGroup, TreeMap::new,
                        Collectors.mapping(t -> new TeamDto(t.getCode(), t.getName(), t.getGroup(),
                                        fans.getOrDefault(t.getCode(), List.of())),
                                Collectors.toList())));
        return byGroup.entrySet().stream()
                .map(e -> new GroupDto(e.getKey(), e.getValue()))
                .toList();
    }

    /** Equipo por código (sin partidos); 404 si no existe. */
    public TeamDto findTeam(String code) {
        return teams.findById(code)
                .map(t -> new TeamDto(t.getCode(), t.getName(), t.getGroup(),
                        fansByTeam().getOrDefault(t.getCode(), List.of())))
                .orElseThrow(() -> new NotFoundException("Equipo no encontrado: " + code));
    }
}
