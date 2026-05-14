package com.example.chatclient.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/tools")
public class ToolProxyController {

    private final RestClient restClient;

    public ToolProxyController(
            @Value("${employee.mcp.url:http://localhost:8082}") String mcpUrl) {
        this.restClient = RestClient.builder().baseUrl(mcpUrl).build();
    }

    @GetMapping
    public ResponseEntity<String> list() {
        try {
            String body = restClient.get().uri("/tools").retrieve().body(String.class);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(body != null ? body : "[]");
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("[]");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody String body) {
        try {
            String response = restClient.post()
                    .uri("/tools/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(response != null ? response : "{}");
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"" + sanitize(e.getMessage()) + "\"}");
        }
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<String> delete(@PathVariable String name) {
        try {
            String response = restClient.delete()
                    .uri("/tools/" + name)
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(response != null ? response : "{\"deleted\":true}");
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"" + sanitize(e.getMessage()) + "\"}");
        }
    }

    private String sanitize(String msg) {
        return msg == null ? "unknown error" : msg.replace("\"", "'");
    }
}
