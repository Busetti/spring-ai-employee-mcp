package com.example.chatclient.service;

import org.springframework.ai.chat.client.ChatClient;
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

    public ChatService(ChatModel chatModel, SyncMcpToolCallbackProvider toolCallbackProvider) {
        this.chatClient = ChatClient.create(chatModel);
        this.toolCallbackProvider = toolCallbackProvider;
    }

    private static final String SYSTEM_PROMPT = """
            You are an Employee Management Assistant. Use the provided tools to answer questions.
            Rules:
            1. Call the relevant tool first to get live data.
            2. ALWAYS write a clear, human-readable text answer after receiving tool results. Never return empty output.
            3. For salary questions, call get-all-employees then compute from the results.
            4. If a tool returns an error, explain it to the user in plain text.
            5. Always end with a complete sentence summarising what you found.
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

    public String chat(String message, List<String> selectedToolNames) {
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
        String content = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .toolCallbacks(filtered)
                .call()
                .content();
        return (content != null && !content.isBlank()) ? content
                : "I retrieved the data but couldn't generate a summary. Please try rephrasing your question.";
    }
}
