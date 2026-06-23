package com.wirbi.mundial.controller;

import com.wirbi.mundial.dto.ChatMessageDto;
import com.wirbi.mundial.dto.ChatMessageRequest;
import com.wirbi.mundial.dto.ChatReactionRequest;
import com.wirbi.mundial.service.ChatService;
import com.wirbi.mundial.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** La Tribuna (piloto beta): todos los endpoints exigen betaAccess (403 si no). */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chat;
    private final CurrentUserService currentUser;

    public ChatController(ChatService chat, CurrentUserService currentUser) {
        this.chat = chat;
        this.currentUser = currentUser;
    }

    @GetMapping("/messages")
    public List<ChatMessageDto> messages() {
        return chat.list(currentUser.currentUser());
    }

    @PostMapping("/messages")
    public ChatMessageDto post(@Valid @RequestBody ChatMessageRequest body) {
        return chat.post(currentUser.currentUser(), body.text());
    }

    @PostMapping("/messages/{id}/reactions")
    public ChatMessageDto react(@PathVariable String id, @Valid @RequestBody ChatReactionRequest body) {
        return chat.react(currentUser.currentUser(), id, body.emoji());
    }
}
