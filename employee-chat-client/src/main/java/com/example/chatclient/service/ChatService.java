package com.example.chatclient.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final SyncMcpToolCallbackProvider toolCallbackProvider;

    public ChatService(ChatModel chatModel, SyncMcpToolCallbackProvider toolCallbackProvider) {
        this.chatClient = ChatClient.create(chatModel);
        this.toolCallbackProvider = toolCallbackProvider;
    }

    private static final String SYSTEM_PROMPT = """
            You are an Employee Management Assistant. You MUST call the provided tools to answer every question.
            
            Available tools and when to use them:
            - get-all-employees        → use for: list all, show all, count, salary analysis, grouping by dept
            - get-employee-by-id       → use for: find by ID number
            - get-employees-by-department → use for: filter by department name (Engineering, HR, Finance, Marketing)
            - get-employees-by-role    → use for: filter by job title
            - create-employee          → use for: add / create a new employee
            - update-employee          → use for: update / change employee details
            - delete-employee          → use for: remove / delete an employee
            
            Rules:
            1. ALWAYS invoke a tool first. Never answer from memory.
            2. After getting tool results, format the answer clearly for the user.
            3. For salary questions (average, highest, lowest), call get-all-employees then compute from results.
            4. Never say "I cannot access data" or "no data provided" — you have live access via tools.
            """;

    public String chat(String message) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .toolCallbacks(toolCallbackProvider.getToolCallbacks())
                .call()
                .content();
    }
}
