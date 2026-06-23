package com.wirbi.mundial.controller;

import com.wirbi.mundial.service.CurrentUserService;
import com.wirbi.mundial.service.MatchService;
import com.wirbi.mundial.dto.MatchDto;
import com.wirbi.mundial.dto.MatchPredictionDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matches;
    private final CurrentUserService currentUser;

    public MatchController(MatchService matches, CurrentUserService currentUser) {
        this.matches = matches;
        this.currentUser = currentUser;
    }

    /** Feed de partidos ordenado cronológicamente, con estado derivado. */
    @GetMapping
    public List<MatchDto> list() {
        return matches.list();
    }

    /** Pronósticos de todos para un partido (409 mientras siga abierto). */
    @GetMapping("/{id}/predictions")
    public List<MatchPredictionDto> predictions(@PathVariable String id) {
        return matches.predictionsFor(id, currentUser.currentUserId());
    }
}
