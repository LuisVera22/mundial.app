package com.wirbi.mundial.service;

import com.wirbi.mundial.dto.ChatMessageDto;
import com.wirbi.mundial.dto.ChatReactionDto;
import com.wirbi.mundial.exception.ConflictException;
import com.wirbi.mundial.exception.NotFoundException;
import com.wirbi.mundial.model.ChatMessage;
import com.wirbi.mundial.model.ChatReaction;
import com.wirbi.mundial.model.User;
import com.wirbi.mundial.repository.ChatMessageRepository;
import com.wirbi.mundial.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La Tribuna: sala global efímera, abierta a todos los participantes.
 *  - Mensajes de texto plano ≤280; se autodestruyen a las 24 h (índice TTL).
 *  - Rate limit anti-spam: 1 mensaje cada 5 s por usuario.
 *  - Reacciones de un set fijo, toggle por usuario.
 *  - rank/points se congelan al escribir (LeaderboardService).
 */
@Service
public class ChatService {

    /**
     * Set curado del piloto: emojis del sistema, sin librerías. Es la única
     * fuente de verdad de qué puede llegar como reacción (whitelist espejo de
     * EMOJIS en Tribuna.tsx — mantener idénticos byte a byte).
     */
    public static final Set<String> ALLOWED_EMOJIS = Set.of(
            "⚽", "🔥", "😂", "💀", "👏",
            "🏆", "🥇", "🥈", "🥉", "🥅", "🧤", "🟨", "🟥", "🐐",
            "🎉", "🥳", "🙌", "👍", "👎", "🤝", "💪", "💯", "⭐",
            "😅", "🤣", "😍", "🥰", "😎", "🤩", "😭", "😢", "😡",
            "😱", "🤯", "🤔", "🙄", "😬", "🤡", "😴", "👀", "🙈",
            "❤️", "💔", "🍿", "🧊", "🚀", "😤", "🤞", "✌️");

    private static final Duration RATE_LIMIT = Duration.ofSeconds(5);

    private final ChatMessageRepository messages;
    private final UserRepository users;
    private final LeaderboardService leaderboard;
    private final Clock clock;

    public ChatService(ChatMessageRepository messages, UserRepository users,
                       LeaderboardService leaderboard, Clock clock) {
        this.messages = messages;
        this.users = users;
        this.leaderboard = leaderboard;
        this.clock = clock;
    }

    /** Mensajes en orden cronológico (viejo → nuevo, listo para pintar). */
    public List<ChatMessageDto> list(User current) {
        Map<String, User> userMap = users.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return messages.findTop100ByOrderByCreatedAtDesc().stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
                .map(m -> toDto(m, userMap.get(m.getUserId()), current.getId()))
                .filter(dto -> dto != null)
                .toList();
    }

    public ChatMessageDto post(User current, String text) {
        String clean = text == null ? "" : text.trim();
        if (clean.isEmpty()) throw new ConflictException("El mensaje no puede estar vacío.");
        if (clean.length() > 280) throw new ConflictException("Máximo 280 caracteres.");

        Instant now = Instant.now(clock);
        messages.findTopByUserIdOrderByCreatedAtDesc(current.getId()).ifPresent(last -> {
            if (Duration.between(last.getCreatedAt(), now).compareTo(RATE_LIMIT) < 0) {
                throw new ConflictException("Muy rápido: espera unos segundos entre mensajes.");
            }
        });

        RankedUser row = leaderboard.rowFor(current.getId());
        ChatMessage saved = messages.save(new ChatMessage(null, current.getId(), clean,
                row != null ? row.rank() : null,
                row != null ? row.points() : null,
                new ArrayList<>(), now));
        return toDto(saved, current, current.getId());
    }

    /** Toggle de reacción: si ya estaba, la quita; si no, la agrega. */
    public ChatMessageDto react(User current, String messageId, String emoji) {
        if (!ALLOWED_EMOJIS.contains(emoji)) {
            throw new ConflictException("Reacción no permitida.");
        }
        ChatMessage m = messages.findById(messageId)
                .orElseThrow(() -> new NotFoundException("El mensaje ya no existe (los mensajes duran 24 h)."));
        List<ChatReaction> reactions = m.getReactions() != null ? new ArrayList<>(m.getReactions()) : new ArrayList<>();
        ChatReaction mine = new ChatReaction(current.getId(), emoji);
        if (!reactions.remove(mine)) reactions.add(mine);
        m.setReactions(reactions);
        ChatMessage saved = messages.save(m);
        User author = users.findById(saved.getUserId()).orElse(null);
        return toDto(saved, author, current.getId());
    }

    private ChatMessageDto toDto(ChatMessage m, User author, String currentUserId) {
        if (author == null) return null; // autor borrado → mensaje huérfano, se omite

        // Agregar reacciones por emoji preservando el orden de aparición.
        Map<String, List<ChatReaction>> byEmoji = (m.getReactions() == null ? List.<ChatReaction>of() : m.getReactions())
                .stream()
                .collect(Collectors.groupingBy(ChatReaction::emoji, LinkedHashMap::new, Collectors.toList()));
        List<ChatReactionDto> reactions = byEmoji.entrySet().stream()
                .map(e -> new ChatReactionDto(e.getKey(), e.getValue().size(),
                        e.getValue().stream().anyMatch(r -> r.userId().equals(currentUserId))))
                .toList();

        return new ChatMessageDto(m.getId(), author.getName(), author.getHue(), author.getAvatar(),
                m.getRank(), m.getPoints(), m.getText(), m.getCreatedAt(),
                m.getUserId().equals(currentUserId), reactions);
    }
}
