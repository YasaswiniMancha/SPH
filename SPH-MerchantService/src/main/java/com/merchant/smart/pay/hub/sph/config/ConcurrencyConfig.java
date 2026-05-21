package com.merchant.smart.pay.hub.sph.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
public class ConcurrencyConfig {

    /**
     * Thread pool for async database operations
     */
    @Bean(name = "merchantAsyncExecutor")
    public Executor merchantAsyncExecutor() {
        log.info("Configuring merchant async executor");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("merchant-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool for kafka event publishing
     */
    @Bean(name = "kafkaEventExecutor")
    public Executor kafkaEventExecutor() {
        log.info("Configuring kafka event executor");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("kafka-event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool for scheduled tasks
     */
    @Bean(name = "merchantTaskScheduler")
    public ThreadPoolTaskScheduler merchantTaskScheduler() {
        log.info("Configuring merchant task scheduler");
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("merchant-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        return scheduler;
    }

    /**
     * Virtual threads executor (Java 19+)
     */
    @Bean(name = "virtualThreadExecutor")
    public ExecutorService virtualThreadExecutor() {
        log.info("Configuring virtual thread executor (Java 21+)");
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}