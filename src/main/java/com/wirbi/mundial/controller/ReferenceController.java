package com.wirbi.mundial.controller;

import com.wirbi.mundial.dto.GroupDto;
import com.wirbi.mundial.dto.PlayerDto;
import com.wirbi.mundial.dto.TeamDetailDto;
import com.wirbi.mundial.dto.TeamDto;
import com.wirbi.mundial.service.MatchService;
import com.wirbi.mundial.service.ReferenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Datos de referencia: equipos, jugadores, grupos y detalle de equipo. */
@RestController
@RequestMapping("/api")
public class ReferenceController {

    private final ReferenceService reference;
    private final MatchService matches;

    public ReferenceController(ReferenceService reference, MatchService matches) {
        this.reference = reference;
        this.matches = matches;
    }

    @GetMapping("/teams")
    public List<TeamDto> teams() {
        return reference.allTeams();
    }

    @GetMapping("/players")
    public List<PlayerDto> players() {
        return reference.allPlayers();
    }

    /** Grupos del torneo (A–L) con sus selecciones. */
    @GetMapping("/groups")
    public List<GroupDto> groups() {
        return reference.groups();
    }

    /** Detalle de una selección + sus partidos (programados y jugados). */
    @GetMapping("/teams/{code}")
    public TeamDetailDto team(@PathVariable String code) {
        TeamDto t = reference.findTeam(code);
        return new TeamDetailDto(t.code(), t.name(), t.group(), t.fans(), matches.matchesForTeam(code));
    }
}
