package org.tars.core.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as idempotent. Duplicate requests with the same key will return cached result.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * SpEL expression to compute the idempotency key from method arguments.
     */
    String key();

    /**
     * TTL in seconds for the idempotency record.
     */
    long ttlSeconds() default 86400; // 24h
}
