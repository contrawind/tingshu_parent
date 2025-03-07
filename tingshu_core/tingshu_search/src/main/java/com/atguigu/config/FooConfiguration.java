package com.atguigu.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FooConfiguration {
    @Bean
    Logger.Level feignLoggerLever() {
        return Logger.Level.FULL;
    }
}

