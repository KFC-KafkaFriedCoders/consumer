package org.example.consumer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync(proxyTargetClass=true)
public class AsyncConfig {
    
    @Value("${app.async.core-pool-size:5}")
    private int corePoolSize;
    
    @Value("${app.async.max-pool-size:10}")
    private int maxPoolSize;
    
    @Value("${app.async.queue-capacity:25}")
    private int queueCapacity;
    
    @Value("${app.async.thread-name-prefix:async-task-}")
    private String threadNamePrefix;
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.initialize();
        return executor;
    }
}
