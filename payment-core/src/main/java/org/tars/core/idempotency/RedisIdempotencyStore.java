package org.tars.core.idempotency;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis-backed idempotency store for distributed environments.
 */
@Component
@ConditionalOnProperty(name = "payment.idempotency.store", havingValue = "redis", matchIfMissing = true)
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final String PREFIX = "idempotency:";
    private static final String LOCK_PREFIX = "idempotency:lock:";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisIdempotencyStore(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(PREFIX + key);
    }

    @Override
    public void put(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(PREFIX + key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean tryLock(String key, long ttlSeconds) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + key, "LOCKED", ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void release(String key) {
        redisTemplate.delete(LOCK_PREFIX + key);
        redisTemplate.delete(PREFIX + key);
    }
}
