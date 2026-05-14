package com.example.employeemcp.dynamic.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-process token-bucket rate limiter per remote IP.
 * Allows up to MAX_REQUESTS registrations per WINDOW_SECONDS.
 */
@Component
public class RegistrationRateLimiter {

    private static final int MAX_REQUESTS = 20;
    private static final long WINDOW_SECONDS = 60;

    private record Bucket(AtomicInteger count, Instant windowStart) {}

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void checkLimit(String clientIp) {
        Instant now = Instant.now();
        buckets.compute(clientIp, (ip, existing) -> {
            if (existing == null ||
                now.getEpochSecond() - existing.windowStart().getEpochSecond() >= WINDOW_SECONDS) {
                return new Bucket(new AtomicInteger(1), now);
            }
            if (existing.count().incrementAndGet() > MAX_REQUESTS) {
                throw new RateLimitExceededException(
                        "Rate limit exceeded for " + ip + ". Max " + MAX_REQUESTS +
                        " registrations per " + WINDOW_SECONDS + " seconds.");
            }
            return existing;
        });
    }

    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}
