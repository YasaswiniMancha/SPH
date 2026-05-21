package com.auth.smart.pay.hub.sph.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
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

import java.time.Duration;
import java.util.concurrent.Executor;

// Central configuration class for authentication infrastructure components like caching, async execution, and circuit breaker monitoring.
@Configuration
@EnableCaching
@EnableAsync
@EnableScheduling
@Slf4j
public class AuthInfraConfig {

    @Bean
    CacheManager cacheManager(RedisConnectionFactory connectionFactory, @Value("${auth.cache.user.ttl-minutes:10}") long userTtlMinutes) {
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(userTtlMinutes))
            .disableCachingNullValues();
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfiguration)
            .build();
    }

    @Bean(name = "authEventExecutor")
    Executor authEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("auth-events-");
        executor.setAwaitTerminationSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerRegistryEventConsumer(MeterRegistry meterRegistry) {
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
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) { //entryReplacedEvent is triggered when a circuit breaker is replaced in the registry, which can happen during configuration changes or updates. This allows us to log the replacement event and keep track of circuit breaker lifecycle events for monitoring and debugging purposes.
                log.info("CircuitBreaker '{}' replaced", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }

    @Bean
    RedisCacheConfiguration userCacheConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10));  // 10 min TTL
    }

    @Bean //we can use this aspect to automatically time methods annotated with @Timed and have the metrics available in the MeterRegistry for monitoring and alerting.
    TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}