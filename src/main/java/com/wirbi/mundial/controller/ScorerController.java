package com.wirbi.mundial.controller;

import com.wirbi.mundial.dto.ScorerDto;
import com.wirbi.mundial.service.ScorerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Tabla de goleo del torneo (Bota de Oro). */
@RestController
@RequestMapping("/api")
public class ScorerController {

    private final ScorerService scorers;

    public ScorerController(ScorerService scorers) {
        this.scorers = scorers;
    }

    @GetMapping("/scorers")
    public List<ScorerDto> scorers() {
        return scorers.list();
    }
}
