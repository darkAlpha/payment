package org.tars.core.idempotency;

/**
 * Abstraction for idempotency key storage.
 * Implementations: Redis, In-Memory.
 */
public interface IdempotencyStore {

    Object get(String key);

    void put(String key, Object value, long ttlSeconds);

    boolean tryLock(String key, long ttlSeconds);

    void release(String key);
}
