# Spring AI Employee Assistant with MCP

A full-stack AI system built with **Spring Boot 3.4.1 + Spring AI 1.1.0 + Ollama** that exposes employee management as AI-callable tools using the **Model Context Protocol (MCP)**. Features multi-turn conversation history via Spring AI's `MessageChatMemoryAdvisor`, dynamic tool registration, and a dark-themed chat UI.

---

## Architecture

```
Browser
  └── employee-chat-client (:8083)   ←  Chat UI · Ollama qwen2.5:3b · Session Memory
        └── employee-mcp-server (:8082)  ←  MCP Tools (SSE) · Dynamic Tool Registry · H2 file DB
              └── employee-service (:8081)  ←  REST CRUD API · H2 in-memory DB
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 23 |
| Framework | Spring Boot 3.4.1 |
| AI Framework | Spring AI 1.1.0 |
| LLM | Ollama `qwen2.5:3b` (local, no cloud key needed) |
| Protocol | MCP (Model Context Protocol) via SSE |
| Memory | Spring AI `MessageChatMemoryAdvisor` + `MessageWindowChatMemory` |
| Database | H2 in-memory (employee-service) · H2 file (mcp-server dynamic tools) |
| ORM | Spring Data JPA |
| Frontend | Vanilla HTML/CSS/JS (dark theme) |

---

## Services

### 1. `employee-service` — port 8081
Plain Spring Boot REST API. No AI dependencies.
- JPA + H2 in-memory DB, preloaded with 10 employees
- Departments: Engineering, HR, Finance, Marketing
- Full CRUD: GET / POST / PUT / DELETE

### 2. `employee-mcp-server` — port 8082
Spring AI MCP Server (SSE transport). Two tool sources:

**Static tools** (`@McpTool` on `EmployeeMcpService`):

| Tool Name | Operation |
|---|---|
| `get-all-employees` | List all employees |
| `get-employee-by-id` | Find by numeric ID |
| `get-employees-by-department` | Filter by department |
| `get-employees-by-role` | Filter by job role |
| `create-employee` | Create new record |
| `update-employee` | Update existing record |
| `delete-employee` | Delete by ID |

**Dynamic tools** (registered at runtime via REST):
- Stored in H2 file DB (`./data/dynamic-tools`), survive restarts
- Supports GET / POST / PUT / PATCH / DELETE endpoints
- Path params auto-detected from `{param}` in URL
- Optional `systemPrompt` to guide LLM on how to use the tool
- SSRF guard + rate limiter (20 req/60s per IP) on registration

**MCP Extensions** (`EmployeeMcpExtensions`):
- `@McpResource("employees://all")` — plain-text employee list
- `@McpResource("employees://{id}")` — single employee profile
- `@McpPrompt("employee-summary")` — department analysis prompt
- `@McpPrompt("employee-search-prompt")` — employee lookup prompt

### 3. `employee-chat-client` — port 8083
Spring AI Chat Client with per-session conversation memory.
- `SyncMcpToolCallbackProvider` auto-discovers all static + dynamic MCP tools
- `MessageChatMemoryAdvisor` maintains multi-turn history per session
- `ChatSessionStore` manages up to 50 concurrent sessions (auto-prunes oldest)
- Session titles auto-set from first message
- Chat UI with tool selector, dynamic tool registration panel, context prompt bar

---

## Prerequisites

- Java 23+
- Maven 3.9+
- [Ollama](https://ollama.com) installed and running

---

## Quick Start

```bash
# 1. Install Ollama and pull model
brew install ollama
brew services start ollama
ollama pull qwen2.5:3b

# 2. Start employee-service
cd employee-service && mvn spring-boot:run

# 3. Start employee-mcp-server
cd employee-mcp-server && mvn spring-boot:run

# 4. Start employee-chat-client
cd employee-chat-client && mvn spring-boot:run

# 5. Open UI
open http://localhost:8083
```

---

## API Reference

### employee-service — `localhost:8081`

```bash
# List all employees
curl http://localhost:8081/api/employees

# Get by ID
curl http://localhost:8081/api/employees/1

# Filter by department
curl http://localhost:8081/api/employees/department/Engineering

# Filter by role
curl http://localhost:8081/api/employees/role/Software%20Engineer

# Get employee address
curl http://localhost:8081/api/employees/1/address

# Create employee
curl -X POST http://localhost:8081/api/employees \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane@example.com",
    "department": "Engineering",
    "role": "Backend Developer",
    "salary": 85000
  }'

# Update employee
curl -X PUT http://localhost:8081/api/employees/1 \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane@example.com",
    "department": "Engineering",
    "role": "Senior Backend Developer",
    "salary": 95000
  }'

# Delete employee
curl -X DELETE http://localhost:8081/api/employees/1
```

---

### employee-mcp-server — `localhost:8082`

```bash
# List all registered dynamic tools
curl http://localhost:8082/tools

# Register a dynamic GET tool (no path params)
curl -X POST http://localhost:8082/tools/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "list-all-employees",
    "description": "Returns all employees from the employee service",
    "method": "GET",
    "url": "http://localhost:8081/api/employees",
    "timeoutSeconds": 10
  }'

# Register a dynamic GET tool with path param
curl -X POST http://localhost:8082/tools/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "get-employee-dept",
    "description": "Get employees by department name",
    "method": "GET",
    "url": "http://localhost:8081/api/employees/department/{department}",
    "timeoutSeconds": 10,
    "systemPrompt": "Always list employee names and salaries in your response."
  }'

# Register a dynamic GET tool with custom headers
curl -X POST http://localhost:8082/tools/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my-api-tool",
    "description": "Calls an external API with auth header",
    "method": "GET",
    "url": "https://api.example.com/data/{id}",
    "headers": { "Authorization": "Bearer MY_TOKEN" },
    "timeoutSeconds": 15
  }'

# Register a dynamic POST tool with body template
curl -X POST http://localhost:8082/tools/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "create-emp",
    "description": "Create a new employee record",
    "method": "POST",
    "url": "http://localhost:8081/api/employees",
    "requestBodyTemplate": "{\"firstName\":\"{{firstName}}\",\"lastName\":\"{{lastName}}\",\"email\":\"{{email}}\",\"department\":\"{{department}}\",\"role\":\"{{role}}\",\"salary\":{{salary}}}",
    "timeoutSeconds": 10
  }'

# Unregister a dynamic tool
curl -X DELETE http://localhost:8082/tools/get-employee-dept
```

---

### employee-chat-client — `localhost:8083`

#### Chat

```bash
# Basic chat (auto-creates a new session)
curl -X POST http://localhost:8083/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Who works in Engineering?"
  }'

# Chat with session ID for conversation continuity
curl -X POST http://localhost:8083/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What are their salaries?",
    "sessionId": "YOUR_SESSION_ID"
  }'

# Chat with specific tools selected
curl -X POST http://localhost:8083/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "List all employees",
    "sessionId": "YOUR_SESSION_ID",
    "toolNames": ["get-all-employees", "get-employees-by-department"]
  }'

# Chat with user context (prepended to every message)
curl -X POST http://localhost:8083/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Who earns the most?",
    "sessionId": "YOUR_SESSION_ID",
    "userContext": "Focus on Engineering department only. Show salaries in INR."
  }'

# List all available MCP tools
curl http://localhost:8083/api/chat/tools
```

#### Session Management

```bash
# Create a new chat session
curl -X POST http://localhost:8083/api/chat/sessions \
  -H "Content-Type: application/json" \
  -d '{"title": "Engineering Analysis"}'

# List all sessions (sorted by last active)
curl http://localhost:8083/api/chat/sessions

# Get full conversation history for a session
curl http://localhost:8083/api/chat/sessions/YOUR_SESSION_ID/history

# Clear conversation history (keep session)
curl -X POST http://localhost:8083/api/chat/sessions/YOUR_SESSION_ID/clear

# Delete session entirely
curl -X DELETE http://localhost:8083/api/chat/sessions/YOUR_SESSION_ID
```

#### Dynamic Tool Management (proxy to MCP server)

```bash
# List dynamic tools (via chat-client proxy)
curl http://localhost:8083/api/tools

# Register a dynamic tool (via chat-client proxy)
curl -X POST http://localhost:8083/api/tools/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "get-employee-dept",
    "description": "Get employees by department",
    "method": "GET",
    "url": "http://localhost:8081/api/employees/department/{department}",
    "timeoutSeconds": 10
  }'

# Unregister a dynamic tool (via chat-client proxy)
curl -X DELETE http://localhost:8083/api/tools/get-employee-dept
```

---

## Conversation History (Spring AI Implementation)

Each chat session maintains full multi-turn memory using:

- **`MessageWindowChatMemory`** — stores up to 50 messages per session, backed by `InMemoryChatMemoryRepository`
- **`MessageChatMemoryAdvisor`** — automatically injects prior conversation turns into every LLM prompt and saves new turns after each response
- **`ChatSessionStore`** — manages all sessions in-process with UUID keys, auto-prunes oldest sessions when the 50-session limit is reached

Session titles are automatically set from the first message sent. Switching sessions in the UI replays the full conversation history.

> **Note:** Tool call results (raw MCP data) are not stored in memory — only user questions and assistant text answers. The LLM will re-call tools if needed in follow-up questions.

---

## Dynamic Tool Security

| Guard | Detail |
|---|---|
| **SSRF Guard** | Blocks loopback, link-local, private IPs, cloud metadata endpoints. Trusted hosts (`localhost`, `127.0.0.1`) bypass IP checks (configurable via `dynamic.tool.ssrf.trusted-hosts`) |
| **Rate Limiter** | 20 registrations per 60 seconds per client IP (in-process token bucket) |
| **Name Validation** | Regex `^[a-zA-Z][a-zA-Z0-9_-]{1,63}$` |
| **Duplicate Guard** | 409 Conflict if tool name already registered |
| **Persistence** | Dynamic tools survive restarts — stored in H2 file DB and reloaded via `ApplicationReadyEvent` |

---

## MCP Annotations

| Annotation | Location | Purpose |
|---|---|---|
| `@McpTool` | `EmployeeMcpService` | Exposes a method as an AI-callable tool |
| `@McpResource` | `EmployeeMcpExtensions` | Exposes read-only data via URI template |
| `@McpPrompt` | `EmployeeMcpExtensions` | Defines a reusable prompt template |

---

## MCP Inspector

Test MCP tools directly without an LLM:
```bash
npx @modelcontextprotocol/inspector
```
Connect to `http://localhost:8082/sse` to browse all tools, resources, and prompts.

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

## Sample Chat Questions

- *"List all employees"*
- *"Who works in Engineering?"*
- *"Who earns the most?"*
- *"What is the average salary by department?"*
- *"How many people are in HR?"*
- *"Add a new employee: Jane Doe, jane@example.com, Engineering, Backend Developer, 85000"*
- *"Update employee 3's salary to 95000"*
- *"Delete employee with ID 5"*
- Follow-up: *"What about their roles?"* — uses conversation memory from previous answer

---

## License
MIT
