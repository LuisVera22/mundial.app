package com.wirbi.mundial.controller;

import com.wirbi.mundial.service.CurrentUserService;
import com.wirbi.mundial.service.LeaderboardService;
import com.wirbi.mundial.dto.LeaderboardDto;
import com.wirbi.mundial.dto.LeaderboardRowDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboard;
    private final CurrentUserService currentUser;

    public LeaderboardController(LeaderboardService leaderboard, CurrentUserService currentUser) {
        this.leaderboard = leaderboard;
        this.currentUser = currentUser;
    }

    @GetMapping
    public LeaderboardDto get() {
        String me = currentUser.currentUserId();
        List<LeaderboardRowDto> rows = leaderboard.ranking().stream()
                .map(r -> new LeaderboardRowDto(r.rank(), r.userId(), r.name(), r.hue(), r.avatar(),
                        r.points(), r.delta(), r.userId().equals(me)))
                .toList();
        return new LeaderboardDto(leaderboard.currentRound(), rows.size(), rows);
    }
}
