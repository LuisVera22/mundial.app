package com.wirbi.mundial.service;

import com.wirbi.mundial.dto.ChatMessageDto;
import com.wirbi.mundial.exception.ConflictException;
import com.wirbi.mundial.model.ChatMessage;
import com.wirbi.mundial.model.ChatReaction;
import com.wirbi.mundial.model.User;
import com.wirbi.mundial.repository.ChatMessageRepository;
import com.wirbi.mundial.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-12T18:00:00Z");

    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final LeaderboardService leaderboard = mock(LeaderboardService.class);
    private final ChatService svc = new ChatService(messages, users, leaderboard,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private final User beta = new User("u1", "Walter", 10, null, true, null, true);

    @Test
    void postSnapshotsRankAndMarksMine() {
        when(messages.findTopByUserIdOrderByCreatedAtDesc("u1")).thenReturn(Optional.empty());
        when(leaderboard.rowFor("u1")).thenReturn(
                new RankedUser("u1", "Walter", 10, null, 3, 12, 10, 2, 2, 4, 1));
        when(messages.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatMessageDto dto = svc.post(beta, "  ¡Vamos México!  ");

        assertEquals("¡Vamos México!", dto.text()); // trim
        assertEquals(3, dto.rank());
        assertEquals(12, dto.points());
        assertTrue(dto.mine());
    }

    @Test
    void postRateLimitsBackToBackMessages() {
        ChatMessage reciente = new ChatMessage("m0", "u1", "hola", null, null,
                new ArrayList<>(), NOW.minusSeconds(2));
        when(messages.findTopByUserIdOrderByCreatedAtDesc("u1")).thenReturn(Optional.of(reciente));

        assertThrows(ConflictException.class, () -> svc.post(beta, "otro mensaje"));
    }

    @Test
    void reactTogglesOnAndOff() {
        ChatMessage msg = new ChatMessage("m1", "u1", "golazo", 1, 6, new ArrayList<>(), NOW);
        when(messages.findById("m1")).thenReturn(Optional.of(msg));
        when(messages.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(users.findById("u1")).thenReturn(Optional.of(beta));

        ChatMessageDto on = svc.react(beta, "m1", "🔥");
        assertEquals(1, on.reactions().size());
        assertEquals(1, on.reactions().get(0).count());
        assertTrue(on.reactions().get(0).mine());

        // El mensaje ya tiene la reacción → segundo toggle la quita
        msg.setReactions(new ArrayList<>(List.of(new ChatReaction("u1", "🔥"))));
        ChatMessageDto off = svc.react(beta, "m1", "🔥");
        assertEquals(0, off.reactions().size());
    }

    @Test
    void reactRejectsUnknownEmoji() {
        assertThrows(ConflictException.class, () -> svc.react(beta, "m1", "🍕"));
    }
}
