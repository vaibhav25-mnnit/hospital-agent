package com.vaibhavs.hospital_agent.controllers;

import com.vaibhavs.hospital_agent.dto.ChatReply;
import com.vaibhavs.hospital_agent.dto.ChatRequest;
import com.vaibhavs.hospital_agent.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService)
    {
        this.chatService = chatService;
    }


    @PostMapping
    public ChatReply chat(@Valid @RequestBody ChatRequest req) {
        String reply =  chatService.chat(req.message());
        return new ChatReply(reply);
    }
}