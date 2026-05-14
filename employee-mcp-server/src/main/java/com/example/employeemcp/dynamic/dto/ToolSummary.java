package com.example.employeemcp.dynamic.dto;

import java.time.Instant;

public class ToolSummary {

    private String name;
    private String description;
    private String method;
    private String url;
    private Instant registeredAt;

    public ToolSummary(String name, String description, String method, String url, Instant registeredAt) {
        this.name = name;
        this.description = description;
        this.method = method;
        this.url = url;
        this.registeredAt = registeredAt;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getMethod() { return method; }
    public String getUrl() { return url; }
    public Instant getRegisteredAt() { return registeredAt; }
}
