package org.tars.core.resilience;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to apply retry + circuit breaker pattern.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Resilient {

    String name() default "default";

    int maxRetries() default 3;

    long retryDelayMs() default 1000;
}
