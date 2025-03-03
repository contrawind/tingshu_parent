package com.atguigu.cache;

import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class TingShuAspect {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RBloomFilter bloomFilter;

    @SneakyThrows
    @Around("@annotation(com.atguigu.cache.TingShuCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint joinPoint) {
        //拿到TingShuCache注解上的值，传的是上什么参数，就把这个参数作为cache的前缀
        //1.拿到目标方法上面的参数
        Object[] methodParams = joinPoint.getArgs();
        //2.拿到目标方法
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = methodSignature.getMethod();
        //3.拿到目标方法上面的注解
        TingShuCache tingShuCache = targetMethod.getAnnotation(TingShuCache.class);

        //4.拿到注解的值
        String prefix = tingShuCache.value();
        Object firstParam = methodParams[0];
        //5.拼接key
        String cacheKey = prefix + ":" + firstParam;
        //6.判断缓存中是否有数据
        Object redisObject = redisTemplate.opsForValue().get(cacheKey);
        String lockKey = "lock-" + firstParam;
        //判断是否需要加锁--性能问题
        if (redisObject == null) {
            synchronized (lockKey.intern()) {
                //判断是否需要从数据库中查询数据
                if (redisObject == null) {
                    Object objectDb = joinPoint.proceed();
                    redisTemplate.opsForValue().set(cacheKey, objectDb);
                    return objectDb;
                }
            }
        }
        return redisObject;
    }
}

