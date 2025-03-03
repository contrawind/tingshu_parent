package com.atguigu.controller;

import com.atguigu.util.SleepUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "redisson接口")
@RestController
@RequestMapping("/api/album")
public class RedissonController {

    @Autowired
    private RedissonClient redissonClient;

    @GetMapping("lock")
    public String lock() {
        RLock lock = redissonClient.getLock("lock");
        String uuid = UUID.randomUUID().toString();
        try {
            lock.lock();//上锁
            SleepUtils.sleep(60);
            System.out.println(Thread.currentThread().getName() + "执行业务" + uuid);
        } finally {
            lock.unlock();//释放锁
        }
        return Thread.currentThread().getName() + "执行业务" + uuid;
    }
}

