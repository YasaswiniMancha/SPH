package com.auth.smart.pay.hub.sph.service;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationRateLimiter {

    private final RedisTemplate<String, Object> redisTemplate; // Redis for distributed rate limiting( a system design approach that enforces traffic limits (e.g., 100 requests/minute) globally across multiple servers by storing request counters in a shared, centralized data store) across multiple instances
    private static final String LOGIN_RATE_KEY = "rate:login:"; // Key prefix for login rate limiting in Redis
    private static final String REGISTER_RATE_KEY = "rate:register:"; // Key prefix for register rate limiting in Redis
    private static final long MAX_LOGIN_PER_IP_PER_SEC = 10; // Maximum login attempts allowed per IP address per second
    private static final long MAX_REGISTER_PER_IP_PER_SEC = 5;  // Maximum registration attempts allowed per IP address per second
    private static final long WINDOW_SIZE_MS = 1000;

    @RateLimiter(name = "authRateLimiter")
    @Timed(value = "auth.ratelimit.login", description = "Login rate limit check")
    public boolean checkLoginRateLimit(String clientIp) { // Check if the number of login attempts from the given client IP exceeds the defined limit
        return checkRateLimit(LOGIN_RATE_KEY + clientIp, MAX_LOGIN_PER_IP_PER_SEC);  // Check if the number of login attempts from the given client IP exceeds the defined limit
    }

    @RateLimiter(name = "authRateLimiter")
    @Timed(value = "auth.ratelimit.register", description = "Register rate limit check")
    public boolean checkRegisterRateLimit(String clientIp) {
        return checkRateLimit(REGISTER_RATE_KEY + clientIp, MAX_REGISTER_PER_IP_PER_SEC); // Check if the number of registration attempts from the given client IP exceeds the defined limit
    }

    private boolean checkRateLimit(String key, long maxRequests) {
        try {
            Long count = redisTemplate.opsForValue().increment(key); // Increment the count for the given key in Redis
            
            if (count != null && count == 1) { // If this is the first request, set an expiration time for the key to reset the count after the defined window size
                redisTemplate.expire(key, Duration.ofSeconds(1));
            }
            
            boolean allowed = (count != null) && (count <= maxRequests); // Check if the count exceeds the maximum allowed requests
            
            if (!allowed) { // If the request is not allowed, log a warning with the key and count
                log.warn("Rate limit exceeded for key: {}, with count: {}", key, count);
            }
            return allowed; // Return whether the request is allowed based on the count and maximum allowed requests
        } catch (Exception ex) {
            log.warn("Rate limit check failed: {}", ex.getMessage());
            return true; // Fail open
        }
    }
}