package com.wirbi.mundial.integration;

import com.wirbi.mundial.model.Stage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FootballDataMapperTest {

    @Test
    void teamCodeUsesOverridesThenLowercaseTla() {
        assertEquals("gb-eng", FootballDataMapper.teamCode(new ProviderTeam("1", "England", "ENG")));
        assertEquals("br", FootballDataMapper.teamCode(new ProviderTeam("2", "Brazil", "BRA")));
        assertEquals("za", FootballDataMapper.teamCode(new ProviderTeam("3", "South Africa", "RSA")));
        assertEquals("kr", FootballDataMapper.teamCode(new ProviderTeam("4", "Korea Republic", "KOR")));
        assertEquals("ba", FootballDataMapper.teamCode(new ProviderTeam("6", "Bosnia-Herzegovina", "BIH")));
        assertEquals("cv", FootballDataMapper.teamCode(new ProviderTeam("7", "Cabo Verde", "CPV")));
        assertEquals("cd", FootballDataMapper.teamCode(new ProviderTeam("8", "Congo DR", "COD")));
        // TLA no mapeado → minúsculas (fallback)
        assertEquals("xyz", FootballDataMapper.teamCode(new ProviderTeam("5", "Testlandia", "XYZ")));
    }

    @Test
    void teamCodeFallsBackToNameWhenTlaUnmapped() {
        // TLA raro/no estándar pero el nombre resuelve el código ISO correcto
        assertEquals("cw", FootballDataMapper.teamCode(new ProviderTeam("1", "Curaçao", "CUR")));
        assertEquals("cd", FootballDataMapper.teamCode(new ProviderTeam("2", "DR Congo", null)));
        assertEquals("cv", FootballDataMapper.teamCode(new ProviderTeam("3", "Cape Verde", "CAP")));
    }

    @Test
    void teamCodeNullForTbd() {
        assertNull(FootballDataMapper.teamCode(null));
        assertNull(FootballDataMapper.teamCode(new ProviderTeam("4", "TBD", null)));
    }

    @Test
    void stageMapping() {
        assertEquals(Stage.GROUP, FootballDataMapper.toStage("GROUP_STAGE"));
        assertEquals(Stage.ROUND_OF_32, FootballDataMapper.toStage("LAST_32"));
        assertEquals(Stage.ROUND_OF_16, FootballDataMapper.toStage("LAST_16"));
        assertEquals(Stage.QUARTER_FINAL, FootballDataMapper.toStage("QUARTER_FINALS"));
        assertEquals(Stage.SEMI_FINAL, FootballDataMapper.toStage("SEMI_FINALS"));
        assertEquals(Stage.FINAL, FootballDataMapper.toStage("FINAL"));
        assertEquals(Stage.GROUP, FootballDataMapper.toStage("UNKNOWN"));
        assertEquals(Stage.GROUP, FootballDataMapper.toStage(null));
    }

    @Test
    void localizedNameUsesSpanishThenFallback() {
        assertEquals("México", FootballDataMapper.localizedName("mx", "Mexico"));
        assertEquals("Bosnia y Herzegovina", FootballDataMapper.localizedName("ba", "Bosnia-Herzegovina"));
        assertEquals("Sudáfrica", FootballDataMapper.localizedName("za", "South Africa"));
        // código sin traducción → fallback (nombre del proveedor)
        assertEquals("Testlandia", FootballDataMapper.localizedName("xyz", "Testlandia"));
    }

    @Test
    void groupLetter() {
        assertEquals("A", FootballDataMapper.groupLetter("GROUP_A"));
        assertEquals("H", FootballDataMapper.groupLetter("GROUP_H"));
        assertNull(FootballDataMapper.groupLetter(null));
    }
}
