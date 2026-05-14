package com.example.employeemcp.dynamic.registry;

import com.example.employeemcp.dynamic.dto.ToolRegistrationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DynamicToolCallbackFactory {

    private static final Logger log = LoggerFactory.getLogger(DynamicToolCallbackFactory.class);
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}]+)}");

    private final ObjectMapper objectMapper;

    public DynamicToolCallbackFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ToolCallback build(ToolRegistrationRequest req) {
        return FunctionToolCallback
                .builder(req.getName(), (Map args) -> invoke(req, args))
                .description(req.getDescription())
                .inputType(Map.class)
                .inputSchema(buildInputSchema(req))
                .build();
    }

    private String buildInputSchema(ToolRegistrationRequest req) {
        Set<String> pathParams = new LinkedHashSet<>();
        Matcher m = PATH_PARAM_PATTERN.matcher(req.getUrl());
        while (m.find()) {
            pathParams.add(m.group(1));
        }

        StringBuilder props = new StringBuilder();
        boolean first = true;
        for (String param : pathParams) {
            if (!first) props.append(",");
            String desc = param.toLowerCase().contains("id")
                    ? "Numeric " + param + " of the resource. Must be an integer, NOT a name."
                    : "Value for path parameter: " + param;
            props.append("\"").append(param)
                 .append("\":{\"type\":\"string\",\"description\":\"").append(desc).append("\"}");
            first = false;
        }

        StringBuilder required = new StringBuilder();
        first = true;
        for (String param : pathParams) {
            if (!first) required.append(",");
            required.append("\"").append(param).append("\"");
            first = false;
        }

        return "{\"type\":\"object\",\"properties\":{" + props + "},\"required\":[" + required + "]}";
    }

    @SuppressWarnings("unchecked")
    private String invoke(ToolRegistrationRequest req, Map<String, Object> args) {
        try {
            String resolvedUrl = resolvePlaceholders(req.getUrl(), args);
            log.info("Invoking dynamic tool '{}': {} {}", req.getName(), req.getMethod(), resolvedUrl);

            RestClient client = RestClient.builder().build();
            String httpMethod = req.getMethod().toUpperCase();
            Map<String, String> headers = req.getHeaders();

            String response = switch (httpMethod) {
                case "GET" -> {
                    String urlWithQuery = appendQueryParams(resolvedUrl, args, req.getUrl());
                    RestClient.RequestHeadersSpec<?> spec = client.get().uri(URI.create(urlWithQuery));
                    if (headers != null) headers.forEach(spec::header);
                    yield spec.retrieve().body(String.class);
                }
                case "DELETE" -> {
                    RestClient.RequestHeadersSpec<?> spec = client.delete().uri(URI.create(resolvedUrl));
                    if (headers != null) headers.forEach(spec::header);
                    yield spec.retrieve().body(String.class);
                }
                case "POST" -> {
                    RestClient.RequestBodySpec spec = client.post().uri(URI.create(resolvedUrl));
                    if (headers != null) headers.forEach(spec::header);
                    spec.contentType(MediaType.APPLICATION_JSON).body(buildBody(req, args));
                    yield spec.retrieve().body(String.class);
                }
                case "PUT" -> {
                    RestClient.RequestBodySpec spec = client.put().uri(URI.create(resolvedUrl));
                    if (headers != null) headers.forEach(spec::header);
                    spec.contentType(MediaType.APPLICATION_JSON).body(buildBody(req, args));
                    yield spec.retrieve().body(String.class);
                }
                case "PATCH" -> {
                    RestClient.RequestBodySpec spec = client.patch().uri(URI.create(resolvedUrl));
                    if (headers != null) headers.forEach(spec::header);
                    spec.contentType(MediaType.APPLICATION_JSON).body(buildBody(req, args));
                    yield spec.retrieve().body(String.class);
                }
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + httpMethod);
            };

            return response != null ? response : "{}";

        } catch (Exception ex) {
            log.error("Error invoking dynamic tool '{}': {}", req.getName(), ex.getMessage(), ex);
            return "{\"error\": \"" + sanitize(ex.getMessage()) + "\"}";
        }
    }

    private String resolvePlaceholders(String url, Map<String, Object> args) {
        StringBuffer sb = new StringBuffer();
        Matcher m = PATH_PARAM_PATTERN.matcher(url);
        while (m.find()) {
            String key = m.group(1);
            Object val = args.get(key);
            if (val == null) {
                throw new IllegalArgumentException("Missing path parameter: " + key);
            }
            String encoded = URLEncoder.encode(val.toString(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            m.appendReplacement(sb, Matcher.quoteReplacement(encoded));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String appendQueryParams(String resolvedUrl, Map<String, Object> args, String templateUrl) {
        StringBuilder qs = new StringBuilder(resolvedUrl);
        boolean first = !resolvedUrl.contains("?");
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            if (!templateUrl.contains("{" + entry.getKey() + "}")) {
                qs.append(first ? "?" : "&")
                  .append(entry.getKey())
                  .append("=")
                  .append(entry.getValue());
                first = false;
            }
        }
        return qs.toString();
    }

    private String buildBody(ToolRegistrationRequest req, Map<String, Object> args) {
        try {
            if (req.getRequestBodyTemplate() != null && !req.getRequestBodyTemplate().isBlank()) {
                String body = req.getRequestBodyTemplate();
                for (Map.Entry<String, Object> e : args.entrySet()) {
                    body = body.replace("{{" + e.getKey() + "}}", e.getValue().toString());
                }
                return body;
            }
            return objectMapper.writeValueAsString(args);
        } catch (Exception ex) {
            log.warn("Could not serialize request body: {}", ex.getMessage());
            return "{}";
        }
    }

    private String sanitize(String msg) {
        if (msg == null) return "unknown error";
        return msg.replace("\"", "'").replace("\n", " ");
    }
}
