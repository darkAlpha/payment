package org.tars.core.resilience;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tars.core.exception.BusinessException;
import org.tars.core.exception.ErrorCode;

/**
 * AOP aspect implementing retry with exponential backoff.
 */
@Aspect
@Component
public class ResilienceAspect {

    private static final Logger log = LoggerFactory.getLogger(ResilienceAspect.class);

    @Around("@annotation(resilient)")
    public Object applyResilience(ProceedingJoinPoint joinPoint, Resilient resilient) throws Throwable {
        int attempts = 0;
        long delay = resilient.retryDelayMs();
        Throwable lastException = null;

        while (attempts <= resilient.maxRetries()) {
            try {
                return joinPoint.proceed();
            } catch (BusinessException e) {
                // Business exceptions should not be retried
                throw e;
            } catch (Exception e) {
                lastException = e;
                attempts++;
                if (attempts <= resilient.maxRetries()) {
                    log.warn("Retry {}/{} for {} after {}ms - error: {}",
                            attempts, resilient.maxRetries(), resilient.name(), delay, e.getMessage());
                    Thread.sleep(delay);
                    delay *= 2; // exponential backoff
                }
            }
        }

        log.error("All {} retries exhausted for {}", resilient.maxRetries(), resilient.name(), lastException);
        throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, resilient.name());
    }
}
