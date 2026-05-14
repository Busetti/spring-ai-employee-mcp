package com.example.chatclient.controller;

import com.example.chatclient.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/tools")
    public List<Map<String, String>> listTools() {
        return chatService.listTools();
    }

    @PostMapping
    @SuppressWarnings("unchecked")
    public Map<String, String> chat(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        List<String> toolNames = (List<String>) request.get("toolNames");
        String response = chatService.chat(message, toolNames);
        return Map.of("response", response);
    }
}
