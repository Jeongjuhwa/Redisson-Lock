package com.example.lock.aop;

import com.example.lock.aop.annotation.DistributedLock;
import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAop {

    private static final String REDISSION_LOCK_KEY_PREFIX = "lock:";

    private final RedissonClient redissonClient;
    private final AopTransaction aopTransaction;

    @Around("@annotation(com.example.lock.aop.annotation.DistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        String key = REDISSION_LOCK_KEY_PREFIX + distributedLock.key();
        RLock rLock = redissonClient.getLock(key);

        try {
            boolean isLock = rLock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(),
                distributedLock.timeUnit());
            if (!isLock) {
                return false;
            }

            return aopTransaction.proceed(joinPoint);
        } catch (InterruptedException e) {
            throw new InterruptedException(e.getMessage());
        } finally {
            try {
                rLock.unlock();
            } catch (IllegalMonitorStateException e) {
                log.info("Redisson lock already unlocked");
            }
        }

    }

}
