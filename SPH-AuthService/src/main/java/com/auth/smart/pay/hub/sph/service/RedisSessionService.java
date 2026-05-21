package com.auth.smart.pay.hub.sph.service;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisSessionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user:sessions:";

    @Timed(value = "auth.session.create", description = "Time to create session in Redis")
    public void createSession(String sessionId, String userId, String accessToken, long expirySeconds) {
        Map<String, Object> session = new HashMap<>();
        session.put("userId", userId);
        session.put("accessToken", accessToken);
        session.put("createdAt", System.currentTimeMillis());

        // Store session with TTL
        redisTemplate.opsForHash().putAll(SESSION_PREFIX + sessionId, session);
        redisTemplate.expire(SESSION_PREFIX + sessionId, Duration.ofSeconds(expirySeconds));

        // Track user sessions for quick logout
        redisTemplate.opsForSet().add(USER_SESSIONS_PREFIX + userId, sessionId);
        redisTemplate.expire(USER_SESSIONS_PREFIX + userId, Duration.ofSeconds(expirySeconds));

        log.debug("Session created: {}", sessionId);
    }

    @Timed(value = "auth.session.get", description = "Time to retrieve session from Redis")
    public Map<String, Object> getSession(String sessionId) {
        Map<Object, Object> rawMap = redisTemplate.opsForHash().entries(SESSION_PREFIX + sessionId);
        Map<String, Object> session = new HashMap<>();
        rawMap.forEach((key, value) -> session.put(key.toString(), value));
        return session;
    }

    @Timed(value = "auth.session.validate", description = "Time to validate session")
    public boolean isSessionValid(String sessionId) {
        return redisTemplate.hasKey(SESSION_PREFIX + sessionId);
    }

    @Timed(value = "auth.session.invalidate", description = "Time to invalidate session")
    public void invalidateSession(String sessionId) {
        redisTemplate.delete(SESSION_PREFIX + sessionId);
    }

    @Timed(value = "auth.session.logout.user", description = "Time to logout all user sessions")
    public void logoutAllUserSessions(String userId) {
        // Get all sessions for user
        var sessionIds = redisTemplate.opsForSet().members(USER_SESSIONS_PREFIX + userId);
        
        if (sessionIds != null && !sessionIds.isEmpty()) {
            sessionIds.forEach(sessionId -> {
                redisTemplate.delete(SESSION_PREFIX + sessionId.toString());
            });
        }
        
        // Clear user sessions set
        redisTemplate.delete(USER_SESSIONS_PREFIX + userId);
        
        log.info("All sessions invalidated for user: {}", userId);
    }
}