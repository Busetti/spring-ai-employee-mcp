package com.example.employeemcp.dynamic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

public class ToolRegistrationRequest {

    @NotBlank(message = "Tool name is required")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_-]{1,63}$",
             message = "Tool name must start with a letter and contain only letters, digits, hyphens, or underscores (max 64 chars)")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "HTTP method is required")
    @Pattern(regexp = "^(GET|POST|PUT|PATCH|DELETE)$",
             message = "HTTP method must be one of: GET, POST, PUT, PATCH, DELETE")
    private String method;

    @NotBlank(message = "URL is required")
    private String url;

    private Map<String, String> headers;

    private String requestBodyTemplate;

    private int timeoutSeconds = 10;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

    public String getRequestBodyTemplate() { return requestBodyTemplate; }
    public void setRequestBodyTemplate(String requestBodyTemplate) { this.requestBodyTemplate = requestBodyTemplate; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
