package com.wirbi.mundial.controller;

import com.wirbi.mundial.model.User;
import com.wirbi.mundial.service.CurrentUserService;
import com.wirbi.mundial.service.GlobalPicksAppService;
import com.wirbi.mundial.service.LeaderboardService;
import com.wirbi.mundial.service.PredictionService;
import com.wirbi.mundial.service.RankedUser;
import com.wirbi.mundial.dto.AvatarRequest;
import com.wirbi.mundial.dto.GlobalPicksDto;
import com.wirbi.mundial.dto.GlobalPicksRequest;
import com.wirbi.mundial.dto.MeDto;
import com.wirbi.mundial.dto.OnboardingRequest;
import com.wirbi.mundial.dto.PredictionDto;
import com.wirbi.mundial.dto.PredictionRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints del usuario actual (resuelto por {@link CurrentUserService}). */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final CurrentUserService currentUser;
    private final PredictionService predictions;
    private final GlobalPicksAppService globalPicks;
    private final LeaderboardService leaderboard;

    public MeController(CurrentUserService currentUser, PredictionService predictions,
                        GlobalPicksAppService globalPicks, LeaderboardService leaderboard) {
        this.currentUser = currentUser;
        this.predictions = predictions;
        this.globalPicks = globalPicks;
        this.leaderboard = leaderboard;
    }

    /** Perfil con puntos/rango/estadísticas ya calculados en servidor. */
    @GetMapping
    public MeDto me() {
        return toDto(currentUser.currentUser());
    }

    /** Cambia el avatar elegido (se refleja también en el ranking). */
    @PutMapping("/avatar")
    public MeDto saveAvatar(@Valid @RequestBody AvatarRequest body) {
        return toDto(currentUser.updateAvatar(body.avatar()));
    }

    private MeDto toDto(User u) {
        RankedUser r = leaderboard.rowFor(u.getId());
        return new MeDto(u.getId(), u.getName(), u.getHue(), u.getAvatar(), u.isOnboardingDone(),
                r != null ? r.points() : 0,
                r != null ? r.rank() : null,
                r != null ? r.exactCount() : 0,
                r != null ? r.trendCount() : 0,
                r != null ? r.matchPoints() : 0,
                globalPicks.get(u),
                u.isBetaAccess());
    }

    @GetMapping("/predictions")
    public List<PredictionDto> myPredictions() {
        return predictions.mine(currentUser.currentUserId());
    }

    @PutMapping("/predictions/{matchId}")
    public PredictionDto savePrediction(@PathVariable String matchId,
                                        @Valid @RequestBody PredictionRequest body) {
        return predictions.upsert(currentUser.currentUserId(), matchId, body.home(), body.away());
    }

    @GetMapping("/global-picks")
    public GlobalPicksDto myGlobalPicks() {
        return globalPicks.get(currentUser.currentUser());
    }

    @PutMapping("/global-picks")
    public GlobalPicksDto saveGlobalPicks(@RequestBody GlobalPicksRequest body) {
        return globalPicks.update(currentUser.currentUser(), body.champion(), body.scorer());
    }

    @PostMapping("/onboarding")
    public GlobalPicksDto onboarding(@Valid @RequestBody OnboardingRequest body) {
        return globalPicks.onboarding(currentUser.currentUser(), body.champion(), body.scorer());
    }
}
