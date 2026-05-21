package com.merchant.smart.pay.hub.sph.service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.merchant.smart.pay.hub.sph.dto.response.MerchantResponseDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;

/**
 * Distributed locking using Redis
 * Essential for multi-instance fintech systems
 */
@Slf4j
@Service
public class DistributedLockService {

    private final StringRedisTemplate redisTemplate;
    private static final String LOCK_PREFIX = "lock:";
    private static final long LOCK_TIMEOUT = 30; // seconds

    public DistributedLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Timed(value = "distributed.lock.acquire", description = "Lock acquisition time")
    public String acquireLock(String key) {
        String lockId = UUID.randomUUID().toString();
        String lockKey = LOCK_PREFIX + key;
        
        try {
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                lockKey,
                lockId,
                LOCK_TIMEOUT,
                TimeUnit.SECONDS
            );

            if (Boolean.TRUE.equals(locked)) {
                log.debug("Lock acquired for key: {} with id: {}", key, lockId);
                return lockId;
            } else {
                log.warn("Failed to acquire lock for key: {}", key);
                return null;
            }
        } catch (Exception e) {
            log.error("Error acquiring lock for key: {}", key, e);
            return null;
        }
    }

    @Timed(value = "distributed.lock.release", description = "Lock release time")
    public boolean releaseLock(String key, String lockId) {
        String lockKey = LOCK_PREFIX + key;

        try {
            String storedLockId = redisTemplate.opsForValue().get(lockKey);
            
            if (lockId.equals(storedLockId)) {
                // FIXED: Changed assignment from Long to Boolean
                Boolean deleteResult = redisTemplate.delete(lockKey);
                boolean released = Boolean.TRUE.equals(deleteResult);
                
                if (released) {
                    log.debug("Lock released for key: {}", key);
                }
                return released;
            } else {
                log.warn("Lock id mismatch for key: {}", key);
                return false;
            }
        } catch (Exception e) {
            log.error("Error releasing lock for key: {}", key, e);
            return false;
        }
    }

    @Timed(value = "distributed.lock.execute", description = "Execute with lock time")
    public <T> T executeWithLock(String key, DistributedLockCallback<T> callback) {
        String lockId = acquireLock(key);

        if (lockId == null) {
            log.warn("Could not acquire lock for key: {}, executing without lock", key);
            return callback.execute();
        }

        try {
            log.debug("Executing operation with lock for key: {}", key);
            return callback.execute();
        } catch (Exception e) {
            log.error("Error executing operation with lock for key: {}", key, e);
            throw e;
        } finally {
            boolean released = releaseLock(key, lockId);
            if (!released) {
                log.warn("Failed to release lock for key: {} with id: {}", key, lockId);
            }
        }
    }

    @FunctionalInterface
    public interface DistributedLockCallback<T> {
        T execute();
    }



}