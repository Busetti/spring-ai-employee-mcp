# Spring AI Employee Assistant with MCP

A full-stack AI system built with Spring Boot + Spring AI + Ollama that exposes employee management as AI-callable tools using the **Model Context Protocol (MCP)**.

---

## Architecture

```
Browser
  └── employee-chat-client (8083)  ←  Chat UI + Ollama llama3.2
        └── employee-mcp-server (8082)  ←  MCP Tools via SSE
              └── employee-service (8081)  ←  REST API + H2
```

---

## Services

### 1. employee-service (port 8081)
Plain Spring Boot REST API. No AI dependencies.
- JPA + H2 in-memory database
- Preloaded with 10 employees across Engineering, HR, Finance, Marketing
- Full CRUD via REST endpoints

### 2. employee-mcp-server (port 8082)
Spring AI MCP Server exposing employee-service as AI tools.
- `@McpTool` — 7 callable tools (get-all, get-by-id, get-by-dept, get-by-role, create, update, delete)
- `@McpResource` — read-only employee list at `employees://all`
- `@McpPrompt` — reusable prompt templates
- SSE transport via `spring-ai-starter-mcp-server-webmvc`

### 3. employee-chat-client (port 8083)
Spring AI Chat Client connecting to the MCP server.
- Ollama `llama3.2:1b` as local LLM (no cloud API needed)
- `SyncMcpToolCallbackProvider` discovers and binds MCP tools automatically
- Chat UI served as static HTML
- REST endpoint: `POST /api/chat`

---

## Prerequisites

- Java 23+
- Maven 3.9+
- [Ollama](https://ollama.com) installed and running

---

## Quick Start

### 1. Install Ollama and pull the model
```bash
brew install ollama
brew services start ollama
ollama pull llama3.2:1b
```

### 2. Start employee-service
```bash
cd employee-service
mvn spring-boot:run
```

### 3. Start employee-mcp-server
```bash
cd employee-mcp-server
mvn spring-boot:run
```

### 4. Start employee-chat-client
```bash
cd employee-chat-client
mvn spring-boot:run
```

### 5. Open the Chat UI
```
http://localhost:8083
```

---

## Testing MCP Tools

Use **MCP Inspector** to test tools directly without an LLM:
```bash
npx @modelcontextprotocol/inspector
```
Connect to `http://localhost:8082/sse` — browse all tools, resources, and prompts.

### Sample questions to ask in the UI
- *"List all employees"*
- *"Who works in Engineering?"*
- *"Who earns the most?"*
- *"Average salary by department"*
- *"Add a new employee: Jane Doe, jane@example.com, Engineering, Backend Developer, 80000"*
- *"Delete employee with ID 5"*

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 23 |
| Framework | Spring Boot 3.4.1 |
| AI Framework | Spring AI 1.1.0 |
| LLM | Ollama llama3.2:1b (local) |
| Protocol | MCP (Model Context Protocol) |
| Transport | SSE (Server-Sent Events) |
| Database | H2 (in-memory) |
| ORM | Spring Data JPA |
| Frontend | Vanilla HTML/CSS/JS |

---

## MCP Annotations Used

| Annotation | Purpose |
|-----------|---------|
| `@McpTool` | Exposes a method as an AI-callable tool |
| `@McpResource` | Exposes read-only data via URI template |
| `@McpPrompt` | Defines a reusable prompt template |

---

## Windsurf / Cascade Integration

Add to `~/.codeium/windsurf/mcp_config.json`:
```json
{
  "mcpServers": {
    "employee-mcp": {
      "serverUrl": "http://localhost:8082/sse"
    }
  }
}
```
Cascade can then query employee data and invoke tools directly from the IDE chat.

---

## License
MIT
