package com.example.employeemcp.dynamic.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "dynamic_tool_config")
public class DynamicToolConfigEntity {

    @Id
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false, length = 2000)
    private String url;

    @Column(length = 4000)
    private String headersJson;

    @Column(length = 4000)
    private String requestBodyTemplate;

    @Column(nullable = false)
    private int timeoutSeconds;

    @Column(nullable = false)
    private Instant registeredAt;

    @Column(length = 4000)
    private String systemPrompt;

    protected DynamicToolConfigEntity() {}

    public DynamicToolConfigEntity(String name, String description, String method, String url,
                                    String headersJson, String requestBodyTemplate,
                                    int timeoutSeconds, Instant registeredAt, String systemPrompt) {
        this.name = name;
        this.description = description;
        this.method = method;
        this.url = url;
        this.headersJson = headersJson;
        this.requestBodyTemplate = requestBodyTemplate;
        this.timeoutSeconds = timeoutSeconds;
        this.registeredAt = registeredAt;
        this.systemPrompt = systemPrompt;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getMethod() { return method; }
    public String getUrl() { return url; }
    public String getHeadersJson() { return headersJson; }
    public String getRequestBodyTemplate() { return requestBodyTemplate; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public Instant getRegisteredAt() { return registeredAt; }
    public String getSystemPrompt() { return systemPrompt; }
}
