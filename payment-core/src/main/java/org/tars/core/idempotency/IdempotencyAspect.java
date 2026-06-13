package org.tars.core.idempotency;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.tars.core.exception.BusinessException;
import org.tars.core.exception.ErrorCode;

/**
 * AOP aspect that enforces idempotency using distributed cache.
 */
@Aspect
@Component
public class IdempotencyAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyAspect.class);
    private final IdempotencyStore idempotencyStore;
    private final ExpressionParser parser = new SpelExpressionParser();

    public IdempotencyAspect(IdempotencyStore idempotencyStore) {
        this.idempotencyStore = idempotencyStore;
    }

    @Around("@annotation(idempotent)")
    public Object enforceIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = resolveKey(joinPoint, idempotent.key());
        log.debug("Idempotency check for key={}", key);

        // Check if already processed
        Object cachedResult = idempotencyStore.get(key);
        if (cachedResult != null) {
            log.info("Duplicate request detected, returning cached result for key={}", key);
            return cachedResult;
        }

        // Try to acquire lock
        if (!idempotencyStore.tryLock(key, idempotent.ttlSeconds())) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST, key);
        }

        try {
            Object result = joinPoint.proceed();
            idempotencyStore.put(key, result, idempotent.ttlSeconds());
            return result;
        } catch (Exception e) {
            idempotencyStore.release(key);
            throw e;
        }
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, String expression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            ((StandardEvaluationContext) context).setVariable(paramNames[i], args[i]);
        }
        return parser.parseExpression(expression).getValue(context, String.class);
    }
}
