package com.example.chatclient.controller;

import com.example.chatclient.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String response = chatService.chat(request.get("message"));
        return Map.of("response", response);
    }

    @GetMapping
    public Map<String, String> chat(@RequestParam String message) {
        String response = chatService.chat(message);
        return Map.of("response", response);
    }
}
