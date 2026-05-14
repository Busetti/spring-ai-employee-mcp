package com.example.employeemcp.dynamic.config;

import org.springframework.context.annotation.Configuration;

/**
 * Minimal configuration for the dynamic tool subsystem.
 *
 * - DynamicToolCallbackProvider is a @Component and is picked up automatically.
 * - Spring AI MCP server auto-configuration aggregates ALL ToolCallbackProvider beans,
 *   so DynamicToolCallbackProvider is merged with the annotation-scanner static tools
 *   (@McpTool on EmployeeMcpService) without any extra wiring.
 * - ObjectMapper is intentionally NOT declared here; Spring Boot's JacksonAutoConfiguration
 *   provides a fully configured one (with JavaTimeModule for java.time.* support).
 */
@Configuration
public class DynamicToolConfig {
}
