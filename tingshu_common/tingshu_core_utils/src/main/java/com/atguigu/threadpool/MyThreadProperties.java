package com.atguigu.threadpool;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties(prefix = "thread.pool")
public class MyThreadProperties {
    private Integer corePoolSize=16;
    private Integer maximumPoolSize=32;
    private Integer keepAliveTime=50;
    private Integer queueLength=100;

}

