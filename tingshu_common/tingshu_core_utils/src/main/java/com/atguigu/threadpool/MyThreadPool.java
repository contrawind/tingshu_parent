package com.atguigu.threadpool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
@EnableConfigurationProperties(MyThreadProperties.class)
public class MyThreadPool {
    @Autowired
    private MyThreadProperties myThreadProperties;

    /**
     * LinkedBlockingQueue
     * 不会引起空间碎片问题
     * ArrayBlockingQueue
     * 会引起空间碎片问题
     */

    @Bean
    public ThreadPoolExecutor myPoolExecutor() {
        // ExecutorService executorService = Executors.newFixedThreadPool(5);
        return new ThreadPoolExecutor(myThreadProperties.getCorePoolSize(),
                myThreadProperties.getMaximumPoolSize(),
                myThreadProperties.getKeepAliveTime(), TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(myThreadProperties.getQueueLength()),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}

