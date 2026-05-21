package com.payments.smart.pay.hub.sph.config;

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


@Configuration
@EnableCaching
@EnableAsync
@EnableScheduling
@Slf4j
public class PaymentInfraConfig {

	@Bean
	CacheManager cacheManager(RedisConnectionFactory connectionFactory, @Value("${payment.cache.ttl-minutes:2}") long cacheTtlMinutes) {
		RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
			.entryTtl(Duration.ofMinutes(cacheTtlMinutes))
			.disableCachingNullValues();
		return RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(cacheConfiguration)
			.build();
	}

	@Bean(name = "paymentEventExecutor")
	Executor paymentEventExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(10);
		executor.setMaxPoolSize(20);
		executor.setQueueCapacity(1000);
		executor.setThreadNamePrefix("payment-events-");
		executor.setAwaitTerminationSeconds(60);
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.initialize();
		return executor;
	}

	@Bean(name = "paymentTransactionExecutor")
	Executor paymentTransactionExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(15);
		executor.setMaxPoolSize(30);
		executor.setQueueCapacity(2000);
		executor.setThreadNamePrefix("payment-txn-");
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
			public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemovedEvent) {
				CircuitBreaker removedEntry = entryRemovedEvent.getRemovedEntry();
				log.info("CircuitBreaker '{}' removed", removedEntry.getName());
			}

			@Override
			public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
				CircuitBreaker replacedEntry = entryReplacedEvent.getNewEntry();
				log.info("CircuitBreaker '{}' replaced", replacedEntry.getName());
			}
		};
	}

	@Bean
	public TimedAspect timedAspect(MeterRegistry registry) {
		return new TimedAspect(registry);
	}
}