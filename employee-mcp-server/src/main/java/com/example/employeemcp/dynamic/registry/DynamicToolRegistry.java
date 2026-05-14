package com.example.employeemcp.dynamic.registry;

import com.example.employeemcp.dynamic.dto.ToolRegistrationRequest;
import com.example.employeemcp.dynamic.dto.ToolSummary;
import com.example.employeemcp.dynamic.persistence.DynamicToolConfigEntity;
import com.example.employeemcp.dynamic.persistence.DynamicToolConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpSyncServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DynamicToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(DynamicToolRegistry.class);

    private final ConcurrentHashMap<String, RegisteredTool> registry = new ConcurrentHashMap<>();
    private final McpSyncServer mcpSyncServer;
    private final DynamicToolConfigRepository repository;
    private final DynamicToolCallbackFactory factory;
    private final ObjectMapper objectMapper;

    public DynamicToolRegistry(@Lazy McpSyncServer mcpSyncServer,
                                DynamicToolConfigRepository repository,
                                DynamicToolCallbackFactory factory,
                                ObjectMapper objectMapper) {
        this.mcpSyncServer = mcpSyncServer;
        this.repository = repository;
        this.factory = factory;
        this.objectMapper = objectMapper;
    }

    public record RegisteredTool(
            ToolCallback callback,
            ToolRegistrationRequest request,
            Instant registeredAt
    ) {}

    /**
     * Reload all persisted tools into memory and the live MCP server on startup.
     * Uses ApplicationReadyEvent so the MCP server bean is fully initialized first.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void reloadOnStartup() {
        List<DynamicToolConfigEntity> persisted = repository.findAll();
        if (persisted.isEmpty()) {
            log.info("No persisted dynamic tools found.");
            return;
        }
        log.info("Reloading {} persisted dynamic tool(s) into MCP server...", persisted.size());
        for (DynamicToolConfigEntity entity : persisted) {
            try {
                ToolRegistrationRequest req = toRequest(entity);
                ToolCallback callback = factory.build(req);
                registry.put(req.getName(), new RegisteredTool(callback, req, entity.getRegisteredAt()));
                mcpSyncServer.addTool(McpToolUtils.toSyncToolSpecification(callback));
                log.info("Reloaded dynamic tool: {}", req.getName());
            } catch (Exception ex) {
                log.error("Failed to reload dynamic tool '{}': {}", entity.getName(), ex.getMessage(), ex);
            }
        }
    }

    public void register(String name, ToolCallback callback, ToolRegistrationRequest request) {
        boolean isUpdate = registry.containsKey(name);
        Instant now = Instant.now();
        registry.put(name, new RegisteredTool(callback, request, now));

        if (isUpdate) {
            try { mcpSyncServer.removeTool(name); } catch (Exception ex) {
                log.warn("Could not remove old MCP tool '{}' before re-registration: {}", name, ex.getMessage());
            }
            log.info("Dynamic tool re-registered (overwritten): {}", name);
        } else {
            log.info("Dynamic tool registered: {}", name);
        }

        mcpSyncServer.addTool(McpToolUtils.toSyncToolSpecification(callback));
        persist(request, now);
    }

    public boolean unregister(String name) {
        RegisteredTool removed = registry.remove(name);
        if (removed != null) {
            mcpSyncServer.removeTool(name);
            repository.deleteById(name);
            log.info("Dynamic tool unregistered and removed from DB: {}", name);
            return true;
        }
        log.warn("Attempted to unregister unknown tool: {}", name);
        return false;
    }

    private void persist(ToolRegistrationRequest req, Instant registeredAt) {
        try {
            String headersJson = req.getHeaders() != null
                    ? objectMapper.writeValueAsString(req.getHeaders()) : null;
            repository.save(new DynamicToolConfigEntity(
                    req.getName(), req.getDescription(), req.getMethod(), req.getUrl(),
                    headersJson, req.getRequestBodyTemplate(),
                    req.getTimeoutSeconds(), registeredAt));
        } catch (Exception ex) {
            log.error("Failed to persist dynamic tool '{}': {}", req.getName(), ex.getMessage(), ex);
        }
    }

    private ToolRegistrationRequest toRequest(DynamicToolConfigEntity entity) throws Exception {
        ToolRegistrationRequest req = new ToolRegistrationRequest();
        req.setName(entity.getName());
        req.setDescription(entity.getDescription());
        req.setMethod(entity.getMethod());
        req.setUrl(entity.getUrl());
        req.setRequestBodyTemplate(entity.getRequestBodyTemplate());
        req.setTimeoutSeconds(entity.getTimeoutSeconds());
        if (entity.getHeadersJson() != null && !entity.getHeadersJson().isBlank()) {
            req.setHeaders(objectMapper.readValue(
                    entity.getHeadersJson(), new TypeReference<Map<String, String>>() {}));
        }
        return req;
    }

    public boolean contains(String name) {
        return registry.containsKey(name);
    }

    public Optional<ToolCallback> getCallback(String name) {
        RegisteredTool rt = registry.get(name);
        return rt == null ? Optional.empty() : Optional.of(rt.callback());
    }

    public List<ToolCallback> allCallbacks() {
        List<ToolCallback> callbacks = new ArrayList<>();
        for (RegisteredTool rt : registry.values()) {
            callbacks.add(rt.callback());
        }
        return Collections.unmodifiableList(callbacks);
    }

    public List<ToolSummary> listTools() {
        List<ToolSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, RegisteredTool> entry : registry.entrySet()) {
            ToolRegistrationRequest req = entry.getValue().request();
            summaries.add(new ToolSummary(
                    req.getName(),
                    req.getDescription(),
                    req.getMethod(),
                    req.getUrl(),
                    entry.getValue().registeredAt()
            ));
        }
        return Collections.unmodifiableList(summaries);
    }

    public int size() {
        return registry.size();
    }
}
