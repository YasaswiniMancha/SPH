package com.wallet.smart.pay.hub.sph.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


//this configuration class is responsible for setting up the infrastructure components of the wallet service, including caching, asynchronous execution, and circuit breaker monitoring. It defines beans for cache management using Redis, thread pool executors for handling wallet events and transactions, and a registry event consumer to monitor circuit breaker events. Additionally, it includes a TimedAspect bean for integrating Micrometer metrics with method execution times.
@Configuration
@EnableCaching
@EnableAsync
@EnableScheduling
@Slf4j
public class WalletInfraConfig {

    @Bean
    CacheManager cacheManager(RedisConnectionFactory connectionFactory, @Value("${wallet.cache.balance.ttl-minutes:2}") long balanceTtlMinutes) {
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(balanceTtlMinutes))
            .disableCachingNullValues();//disable caching of null values to prevent cache pollution with non-existent wallet entries
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfiguration)
            .build();
    }

    @Bean(name = "walletEventExecutor")
    Executor walletEventExecutor() {  //this bean defines a ThreadPoolTaskExecutor for handling wallet-related events asynchronously. It is configured with a core pool size of 10 threads, a maximum pool size of 20 threads, and a queue capacity of 1000 tasks. The thread name prefix is set to "wallet-events-" for easier identification in logs. The executor is also configured to wait for tasks to complete on shutdown, with a timeout of 60 seconds.
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("wallet-events-");
        executor.setAwaitTerminationSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean(name = "walletTransactionExecutor")
    Executor walletTransactionExecutor() { //this bean defines another ThreadPoolTaskExecutor specifically for handling wallet transactions asynchronously. It is configured with a core pool size of 15 threads, a maximum pool size of 30 threads, and a queue capacity of 2000 tasks. The thread name prefix is set to "wallet-txn-" for easier identification in logs. Similar to the previous executor, it is also configured to wait for tasks to complete on shutdown, with a timeout of 60 seconds.
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(15);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("wallet-txn-");
        executor.setAwaitTerminationSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerRegistryEventConsumer(MeterRegistry meterRegistry) { //this bean defines a RegistryEventConsumer for monitoring events related to CircuitBreaker instances in the application. It listens for events such as when a CircuitBreaker is added, removed, or replaced in the registry. When such events occur, it logs the relevant information using the SLF4J logger. This allows for better visibility into the lifecycle of CircuitBreaker instances and can help with debugging and monitoring the application's resilience mechanisms.
        return new RegistryEventConsumer<CircuitBreaker>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                CircuitBreaker addedEntry = entryAddedEvent.getAddedEntry();
                log.info("CircuitBreaker '{}' registered", addedEntry.getName());
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
                CircuitBreaker removedEntry = entryRemoveEvent.getRemovedEntry();
                log.info("CircuitBreaker '{}' unregistered", removedEntry.getName());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
                log.info("CircuitBreaker '{}' replaced", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }

    @Bean
    TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
