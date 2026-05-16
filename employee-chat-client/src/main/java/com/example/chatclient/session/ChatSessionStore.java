package com.example.chatclient.session;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatSessionStore {

    private static final int MAX_SESSIONS = 50;

    public record SessionMeta(String id, String title, Instant createdAt, Instant lastActiveAt) {}

    private record SessionEntry(ChatMemory memory, SessionMeta meta) {}

    private final ConcurrentHashMap<String, SessionEntry> sessions = new ConcurrentHashMap<>();

    public String createSession(String title) {
        pruneIfNeeded();
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        SessionMeta meta = new SessionMeta(id, title != null ? title : "New Chat", now, now);
        ChatMemoryRepository repo = new InMemoryChatMemoryRepository();
        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repo)
                .maxMessages(50)
                .build();
        sessions.put(id, new SessionEntry(memory, meta));
        return id;
    }

    public ChatMemory getMemory(String sessionId) {
        SessionEntry entry = sessions.get(sessionId);
        if (entry == null) return null;
        touchSession(sessionId);
        return entry.memory();
    }

    public boolean exists(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    public void clearSession(String sessionId) {
        SessionEntry entry = sessions.get(sessionId);
        if (entry != null) {
            entry.memory().clear(sessionId);
        }
    }

    public boolean deleteSession(String sessionId) {
        SessionEntry removed = sessions.remove(sessionId);
        if (removed != null) {
            removed.memory().clear(sessionId);
            return true;
        }
        return false;
    }

    public void updateTitle(String sessionId, String title) {
        sessions.computeIfPresent(sessionId, (id, entry) -> {
            SessionMeta updated = new SessionMeta(id, title, entry.meta().createdAt(), entry.meta().lastActiveAt());
            return new SessionEntry(entry.memory(), updated);
        });
    }

    public List<SessionMeta> listSessions() {
        List<SessionMeta> list = new ArrayList<>();
        sessions.values().forEach(e -> list.add(e.meta()));
        list.sort((a, b) -> b.lastActiveAt().compareTo(a.lastActiveAt()));
        return Collections.unmodifiableList(list);
    }

    public List<Map<String, String>> getHistory(String sessionId) {
        SessionEntry entry = sessions.get(sessionId);
        if (entry == null) return List.of();
        List<Message> messages = entry.memory().get(sessionId);
        List<Map<String, String>> result = new ArrayList<>();
        for (Message msg : messages) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("role", msg.getMessageType().getValue());
            m.put("content", msg.getText());
            result.add(m);
        }
        return result;
    }

    private void touchSession(String sessionId) {
        sessions.computeIfPresent(sessionId, (id, entry) -> {
            SessionMeta old = entry.meta();
            SessionMeta updated = new SessionMeta(id, old.title(), old.createdAt(), Instant.now());
            return new SessionEntry(entry.memory(), updated);
        });
    }

    private void pruneIfNeeded() {
        if (sessions.size() < MAX_SESSIONS) return;
        sessions.values().stream()
                .map(e -> e.meta())
                .sorted((a, b) -> a.lastActiveAt().compareTo(b.lastActiveAt()))
                .limit(sessions.size() - MAX_SESSIONS + 1)
                .forEach(meta -> deleteSession(meta.id()));
    }
}
