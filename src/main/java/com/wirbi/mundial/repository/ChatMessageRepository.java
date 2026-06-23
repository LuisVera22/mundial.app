package com.wirbi.mundial.repository;

import com.wirbi.mundial.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /** Últimos N mensajes (el TTL ya poda a 24 h; esto acota la respuesta). */
    List<ChatMessage> findTop100ByOrderByCreatedAtDesc();

    /** Último mensaje del usuario (para el rate limit anti-spam). */
    Optional<ChatMessage> findTopByUserIdOrderByCreatedAtDesc(String userId);
}
