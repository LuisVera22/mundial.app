package com.wirbi.mundial.service;

import com.wirbi.mundial.dto.GroupDto;
import com.wirbi.mundial.exception.NotFoundException;
import com.wirbi.mundial.model.GlobalPicks;
import com.wirbi.mundial.model.Team;
import com.wirbi.mundial.model.User;
import com.wirbi.mundial.repository.PlayerRepository;
import com.wirbi.mundial.repository.TeamRepository;
import com.wirbi.mundial.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReferenceServiceTest {

    private final TeamRepository teams = mock(TeamRepository.class);
    private final PlayerRepository players = mock(PlayerRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ReferenceService svc = new ReferenceService(teams, players, users);

    @Test
    void groupsAreOrderedAndOmitTeamsWithoutGroup() {
        when(teams.findAll()).thenReturn(List.of(
                new Team("ar", "Argentina", "D", null),
                new Team("fr", "Francia", "D", null),
                new Team("br", "Brasil", "E", null),
                new Team("tbd", "TBD", null, null))); // sin grupo → se omite

        List<GroupDto> groups = svc.groups();

        assertEquals(2, groups.size());
        assertEquals("D", groups.get(0).group());          // orden A→L
        assertEquals(List.of("Argentina", "Francia"),       // equipos ordenados por nombre
                groups.get(0).teams().stream().map(t -> t.name()).toList());
        assertEquals("E", groups.get(1).group());
        assertEquals(1, groups.get(1).teams().size());
    }

    @Test
    void groupsAttachChampionFansToTheirTeam() {
        when(teams.findAll()).thenReturn(List.of(
                new Team("ar", "Argentina", "D", null),
                new Team("fr", "Francia", "D", null)));
        when(users.findAll()).thenReturn(List.of(
                new User("u1", "Walter", 10, null, true, new GlobalPicks("ar", null, null, null), false),
                new User("u2", "Ana", 20, "a1.png", true, new GlobalPicks("ar", null, null, null), false),
                new User("u3", "Luis", 30, null, true, GlobalPicks.empty(), false))); // sin pick → no cuenta

        List<GroupDto> groups = svc.groups();

        var argentina = groups.get(0).teams().get(0);
        assertEquals(List.of("Ana", "Walter"), // hinchas en orden alfabético
                argentina.fans().stream().map(f -> f.name()).toList());
        var francia = groups.get(0).teams().get(1);
        assertEquals(0, francia.fans().size());
    }

    @Test
    void findTeamReturnsOrThrows() {
        when(teams.findById("ar")).thenReturn(Optional.of(new Team("ar", "Argentina", "D", null)));
        when(teams.findById("zzz")).thenReturn(Optional.empty());

        assertEquals("Argentina", svc.findTeam("ar").name());
        assertThrows(NotFoundException.class, () -> svc.findTeam("zzz"));
    }
}
