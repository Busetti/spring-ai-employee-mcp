package com.example.employeemcp.dynamic.controller;

import com.example.employeemcp.dynamic.dto.ToolRegistrationRequest;
import com.example.employeemcp.dynamic.dto.ToolRegistrationResponse;
import com.example.employeemcp.dynamic.dto.ToolSummary;
import com.example.employeemcp.dynamic.registry.DynamicToolCallbackFactory;
import com.example.employeemcp.dynamic.registry.DynamicToolRegistry;
import com.example.employeemcp.dynamic.security.RegistrationRateLimiter;
import com.example.employeemcp.dynamic.security.SsrfGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tools")
public class DynamicToolController {

    private static final Logger log = LoggerFactory.getLogger(DynamicToolController.class);

    private final DynamicToolRegistry registry;
    private final DynamicToolCallbackFactory factory;
    private final SsrfGuard ssrfGuard;
    private final RegistrationRateLimiter rateLimiter;

    public DynamicToolController(DynamicToolRegistry registry,
                                  DynamicToolCallbackFactory factory,
                                  SsrfGuard ssrfGuard,
                                  RegistrationRateLimiter rateLimiter) {
        this.registry = registry;
        this.factory = factory;
        this.ssrfGuard = ssrfGuard;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Register a new dynamic MCP tool at runtime.
     * POST /tools/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody ToolRegistrationRequest request,
                                       HttpServletRequest httpRequest) {
        String clientIp = resolveClientIp(httpRequest);
        try {
            rateLimiter.checkLimit(clientIp);
            ssrfGuard.validate(request.getUrl());

            if (registry.contains(request.getName())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Tool '" + request.getName() + "' is already registered. " +
                                "Unregister it first or use a different name."));
            }

            ToolCallback callback = factory.build(request);
            registry.register(request.getName(), callback, request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ToolRegistrationResponse.success(
                            request.getName(),
                            request.getDescription(),
                            request.getMethod(),
                            request.getUrl()));

        } catch (RegistrationRateLimiter.RateLimitExceededException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected error registering tool: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal error: " + ex.getMessage()));
        }
    }

    /**
     * Unregister a dynamic tool by name.
     * DELETE /tools/{toolName}
     */
    @DeleteMapping("/{toolName}")
    public ResponseEntity<?> unregister(@PathVariable String toolName) {
        if (!registry.unregister(toolName)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Tool '" + toolName + "' not found in dynamic registry."));
        }
        return ResponseEntity.ok(ToolRegistrationResponse.deleted(toolName));
    }

    /**
     * List all currently registered dynamic tools.
     * GET /tools
     */
    @GetMapping
    public ResponseEntity<List<ToolSummary>> listTools() {
        return ResponseEntity.ok(registry.listTools());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
