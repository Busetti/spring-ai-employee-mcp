package com.example.chatclient.service;

import com.example.chatclient.session.ChatSessionStore;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final SyncMcpToolCallbackProvider toolCallbackProvider;
    private final ChatSessionStore sessionStore;

    public ChatService(ChatModel chatModel,
                       SyncMcpToolCallbackProvider toolCallbackProvider,
                       ChatSessionStore sessionStore) {
        this.chatClient = ChatClient.create(chatModel);
        this.toolCallbackProvider = toolCallbackProvider;
        this.sessionStore = sessionStore;
    }

    private static final String SYSTEM_PROMPT = """
            You are an Employee Management Assistant. Use the provided tools to answer questions.
            Rules:
            1. Call the relevant tool first to get live data.
            2. ALWAYS write a clear, human-readable text answer after receiving tool results. Never return empty output.
            3. For salary questions, call get-all-employees then compute from the results.
            4. If a tool returns an error, explain it to the user in plain text.
            5. Always end with a complete sentence summarising what you found.
            6. You have memory of previous messages in this conversation. Use that context when answering follow-up questions.
            """;

    public List<Map<String, String>> listTools() {
        return Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .map(cb -> {
                    Map<String, String> info = new LinkedHashMap<>();
                    info.put("name", cb.getToolDefinition().name());
                    info.put("description", cb.getToolDefinition().description());
                    return info;
                })
                .sorted(Comparator.comparing(m -> m.get("name")))
                .collect(Collectors.toList());
    }

    public String chat(String message, List<String> selectedToolNames, String userContext, String sessionId) {
        ChatMemory memory = sessionStore.getMemory(sessionId);

        ToolCallback[] allCallbacks = toolCallbackProvider.getToolCallbacks();
        ToolCallback[] filtered;
        if (selectedToolNames == null || selectedToolNames.isEmpty()) {
            filtered = allCallbacks;
        } else {
            Set<String> nameSet = new HashSet<>(selectedToolNames);
            filtered = Arrays.stream(allCallbacks)
                    .filter(cb -> nameSet.contains(cb.getToolDefinition().name()))
                    .toArray(ToolCallback[]::new);
        }

        String fullMessage = (userContext != null && !userContext.isBlank())
                ? "Context: " + userContext.strip() + "\n\nQuestion: " + message
                : message;

        String content = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(fullMessage)
                .toolCallbacks(filtered)
                .advisors(MessageChatMemoryAdvisor.builder(memory)
                        .conversationId(sessionId)
                        .build())
                .call()
                .content();

        maybeUpdateSessionTitle(sessionId, message);

        return (content != null && !content.isBlank()) ? content
                : "I retrieved the data but couldn't generate a summary. Please try rephrasing your question.";
    }

    private void maybeUpdateSessionTitle(String sessionId, String firstMessage) {
        List<Map<String, String>> history = sessionStore.getHistory(sessionId);
        if (history.size() <= 2) {
            String title = firstMessage.length() > 50
                    ? firstMessage.substring(0, 47) + "…"
                    : firstMessage;
            sessionStore.updateTitle(sessionId, title);
        }
    }
}
