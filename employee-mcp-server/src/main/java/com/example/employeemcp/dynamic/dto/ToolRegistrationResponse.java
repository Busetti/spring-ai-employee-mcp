package com.example.employeemcp.dynamic.dto;

import java.time.Instant;

public class ToolRegistrationResponse {

    private String name;
    private String description;
    private String method;
    private String url;
    private String status;
    private String message;
    private Instant registeredAt;

    public ToolRegistrationResponse() {}

    public static ToolRegistrationResponse success(String name, String description, String method, String url) {
        ToolRegistrationResponse r = new ToolRegistrationResponse();
        r.name = name;
        r.description = description;
        r.method = method;
        r.url = url;
        r.status = "REGISTERED";
        r.message = "Tool '" + name + "' registered successfully.";
        r.registeredAt = Instant.now();
        return r;
    }

    public static ToolRegistrationResponse deleted(String name) {
        ToolRegistrationResponse r = new ToolRegistrationResponse();
        r.name = name;
        r.status = "UNREGISTERED";
        r.message = "Tool '" + name + "' unregistered successfully.";
        return r;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getMethod() { return method; }
    public String getUrl() { return url; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public Instant getRegisteredAt() { return registeredAt; }
}
