package com.wallet.smart.pay.hub.sph.service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyCacheService {
//this class is used to cache the transaction ids for idempotency checking in the cache database for 24 hours after the transaction is completed
    private static final Duration TTL = Duration.ofHours(24);
    private final StringRedisTemplate redisTemplate;

    public IdempotencyCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<UUID> getTransactionId(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        String key = cacheKey(idempotencyKey);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(value));
    }

    public void putTransactionId(String idempotencyKey, UUID transactionId) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || transactionId == null) {
            return;
        }
        redisTemplate.opsForValue().set(cacheKey(idempotencyKey), transactionId.toString(), TTL);
    }

    private String cacheKey(String idempotencyKey) {
        return "wallet:idempotency:" + idempotencyKey;
    }
}
