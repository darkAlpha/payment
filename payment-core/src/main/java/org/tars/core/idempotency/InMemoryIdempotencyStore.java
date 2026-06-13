package org.tars.core.idempotency;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory idempotency store for development/testing.
 */
@Component
@ConditionalOnProperty(name = "payment.idempotency.store", havingValue = "memory")
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final ConcurrentHashMap<String, Object> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> locks = new ConcurrentHashMap<>();

    @Override
    public Object get(String key) {
        return store.get(key);
    }

    @Override
    public void put(String key, Object value, long ttlSeconds) {
        store.put(key, value);
    }

    @Override
    public boolean tryLock(String key, long ttlSeconds) {
        return locks.putIfAbsent(key, Boolean.TRUE) == null;
    }

    @Override
    public void release(String key) {
        locks.remove(key);
        store.remove(key);
    }
}
