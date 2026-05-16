package com.example.chatclient.controller;

import com.example.chatclient.session.ChatSessionStore;
import com.example.chatclient.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatSessionStore sessionStore;

    public ChatController(ChatService chatService, ChatSessionStore sessionStore) {
        this.chatService = chatService;
        this.sessionStore = sessionStore;
    }

    @GetMapping("/tools")
    public List<Map<String, String>> listTools() {
        return chatService.listTools();
    }

    @PostMapping("/sessions")
    public Map<String, String> newSession(@RequestBody(required = false) Map<String, String> body) {
        String title = body != null ? body.get("title") : null;
        String sessionId = sessionStore.createSession(title);
        return Map.of("sessionId", sessionId);
    }

    @GetMapping("/sessions")
    public List<ChatSessionStore.SessionMeta> listSessions() {
        return sessionStore.listSessions();
    }

    @GetMapping("/sessions/{sessionId}/history")
    public ResponseEntity<List<Map<String, String>>> getHistory(@PathVariable String sessionId) {
        if (!sessionStore.exists(sessionId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(sessionStore.getHistory(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/clear")
    public ResponseEntity<Map<String, String>> clearSession(@PathVariable String sessionId) {
        if (!sessionStore.exists(sessionId)) {
            return ResponseEntity.notFound().build();
        }
        sessionStore.clearSession(sessionId);
        return ResponseEntity.ok(Map.of("status", "cleared", "sessionId", sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, String>> deleteSession(@PathVariable String sessionId) {
        if (!sessionStore.deleteSession(sessionId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("status", "deleted", "sessionId", sessionId));
    }

    @PostMapping
    @SuppressWarnings("unchecked")
    public Map<String, String> chat(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        List<String> toolNames = (List<String>) request.get("toolNames");
        String userContext = (String) request.get("userContext");
        String sessionId = (String) request.get("sessionId");

        if (sessionId == null || !sessionStore.exists(sessionId)) {
            sessionId = sessionStore.createSession(null);
        }

        String response = chatService.chat(message, toolNames, userContext, sessionId);
        return Map.of("response", response, "sessionId", sessionId);
    }
}
