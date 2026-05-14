package com.example.employeemcp.dynamic.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates a URL against SSRF-unsafe targets:
 * - loopback (127.x, ::1)
 * - link-local (169.254.x, fe80::)
 * - RFC-1918 private ranges
 * - cloud metadata endpoints
 * - non-http(s) schemes
 *
 * Hosts listed in dynamic.tool.ssrf.trusted-hosts bypass all IP checks.
 * Use this for known internal services (e.g. localhost in dev/staging).
 */
@Component
public class SsrfGuard {

    private static final List<String> BLOCKED_HOSTS = List.of(
            "169.254.169.254",   // AWS/GCP metadata
            "metadata.google.internal",
            "metadata.azure.internal",
            "100.100.100.200"    // Alibaba Cloud metadata
    );

    private final Set<String> trustedHosts;

    public SsrfGuard(
            @Value("${dynamic.tool.ssrf.trusted-hosts:}") String trustedHostsCsv) {
        this.trustedHosts = Set.copyOf(
                java.util.Arrays.stream(trustedHostsCsv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(s -> s.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toSet())
        );
    }

    public void validate(String url) {
        URI uri;
        try {
            String sanitized = url.replaceAll("\\{[^}]+}", "placeholder");
            uri = new URI(sanitized);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL syntax: " + url, e);
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("https") && !scheme.equals("http")) {
            throw new IllegalArgumentException("Only http/https schemes are allowed. Got: " + scheme);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL must contain a valid host.");
        }

        String lowerHost = host.toLowerCase(Locale.ROOT);

        if (trustedHosts.contains(lowerHost)) {
            return;
        }

        for (String blocked : BLOCKED_HOSTS) {
            if (lowerHost.equals(blocked)) {
                throw new IllegalArgumentException("URL host is blocked for security reasons: " + host);
            }
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress()) {
                throw new IllegalArgumentException("Loopback addresses are not allowed: " + host);
            }
            if (address.isLinkLocalAddress()) {
                throw new IllegalArgumentException("Link-local addresses are not allowed: " + host);
            }
            if (address.isSiteLocalAddress()) {
                throw new IllegalArgumentException(
                        "Private/internal network addresses are not allowed: " + host);
            }
        } catch (IllegalArgumentException rethrow) {
            throw rethrow;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to resolve host '" + host + "': " + e.getMessage(), e);
        }
    }
}
