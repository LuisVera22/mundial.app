package com.wirbi.mundial.controller;

import com.wirbi.mundial.exception.ConflictException;
import com.wirbi.mundial.service.CurrentUserService;
import com.wirbi.mundial.service.GlobalPicksAppService;
import com.wirbi.mundial.service.LeaderboardService;
import com.wirbi.mundial.service.PredictionService;
import com.wirbi.mundial.dto.PredictionDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica la capa web sin Mongo/Docker: validación de inputs y traducción de
 * excepciones (lock → 409). Los servicios se mockean.
 */
@WebMvcTest(MeController.class)
class MeControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    CurrentUserService currentUser;
    @MockBean
    PredictionService predictions;
    @MockBean
    GlobalPicksAppService globalPicks;
    @MockBean
    LeaderboardService leaderboard;

    @Test
    void rejectsNegativeScoreWith400() throws Exception {
        mvc.perform(put("/api/me/predictions/m05")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"home\":-1,\"away\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void savesValidPrediction() throws Exception {
        when(currentUser.currentUserId()).thenReturn("me");
        when(predictions.upsert(eq("me"), eq("m07"), eq(2), eq(1)))
                .thenReturn(new PredictionDto("m07", 2, 1, Instant.parse("2026-06-11T12:00:00Z")));

        mvc.perform(put("/api/me/predictions/m07")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"home\":2,\"away\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value("m07"))
                .andExpect(jsonPath("$.home").value(2));
    }

    @Test
    void lockedMatchReturns409() throws Exception {
        when(currentUser.currentUserId()).thenReturn("me");
        when(predictions.upsert(eq("me"), eq("m05"), anyInt(), anyInt()))
                .thenThrow(new ConflictException("El pronóstico está cerrado para este partido."));

        mvc.perform(put("/api/me/predictions/m05")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"home\":1,\"away\":0}"))
                .andExpect(status().isConflict());
    }
}
