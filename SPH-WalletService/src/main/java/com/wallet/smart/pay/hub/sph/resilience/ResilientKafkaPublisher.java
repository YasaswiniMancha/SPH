package com.wallet.smart.pay.hub.sph.resilience;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.wallet.smart.pay.hub.sph.event.WalletTransactionEvent;
import com.wallet.smart.pay.hub.sph.exception.WalletServiceException;

import java.util.concurrent.CompletableFuture;

/**
 * Resilient Kafka event publisher with circuit breaker and retry mechanisms.
 * Prevents cascading failures when Kafka is unavailable.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientKafkaPublisher {

    private final KafkaTemplate<String, WalletTransactionEvent> kafkaTemplate;
    private static final long SEND_TIMEOUT_MS = 5000;
//this method is responsible for publishing wallet transaction events to Kafka with resilience patterns. It uses Resilience4j annotations to implement a circuit breaker, retry mechanism, and time limiter. The @CircuitBreaker annotation monitors the method for failures and triggers a fallback method if the failure rate exceeds a certain threshold. The @Retry annotation automatically retries the method according to the configured retry policy, which helps to handle transient failures when Kafka is temporarily unavailable. The @Timed annotation creates a Micrometer metric to track the time taken to execute the method, allowing us to monitor the performance of our Kafka publishing and identify any latency issues.
    @CircuitBreaker(name = "kafkaPublisher", fallbackMethod = "publishEventFallback") // Resilience4j will monitor the publishTransactionEvent method for failures. If the failure rate exceeds the configured threshold, the circuit breaker will open, preventing further attempts to publish events to Kafka and instead triggering the fallback method. This helps to protect the system from cascading failures when Kafka is down or experiencing issues.
    @TimeLimiter(name = "kafkaPublisher") // Resilience4j will enforce a time limit on the execution of the publishTransactionEvent method. If the method takes longer than the configured timeout (e.g., 5 seconds), it will be interrupted and treated as a failure, which can trigger the circuit breaker if the failure rate is high. This prevents hanging requests and ensures that the system remains responsive even when Kafka is slow or unresponsive.
    @Retry(name = "kafkaPublisher") // Resilience4j will automatically retry the publishTransactionEvent method according to the retry configuration for "kafkaPublisher". This helps to handle transient failures when Kafka is temporarily unavailable, improving the chances of successful event publication without immediately triggering the circuit breaker.
    @Timed(value = "wallet.kafka.publish", description = "Time taken to publish wallet transaction event to Kafka") // Micrometer metric for monitoring, @Timed annotation will automatically create a metric named "wallet.kafka.publish" that tracks the time taken to execute the publishTransactionEvent method. This allows us to monitor the performance of our Kafka publishing and identify any latency issues.
    public CompletableFuture<Void> publishTransactionEvent(String topic, WalletTransactionEvent event) {
        try {
            log.debug("Publishing wallet transaction event to Kafka topic: {} for transaction: {}",
                    topic, event.transactionId());

            Message<WalletTransactionEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader(KafkaHeaders.KEY, event.walletId().toString())
                    .setHeader("transaction-id", event.transactionId().toString())
                    .setHeader("timestamp", System.currentTimeMillis())
                    .build();
            return kafkaTemplate.send(message).thenAccept(result ->
                    log.info("Kafka event published successfully: {}",
                            event.transactionId()));

    } catch (Exception ex) {
            log.error("Failed to publish Kafka event: {}. Error: {}", event.transactionId(), ex.getMessage(), ex);
            throw new WalletServiceException("Failed to publish Kafka event: " + event.transactionId(), ex);
        }
     }

    // Fallback method for circuit breaker when Kafka is unavailable. This method will be called when the circuit breaker is open, indicating that Kafka is currently experiencing issues. The fallback method logs a warning and relies on the outbox pattern to ensure that the event will be retried later by the outbox relay task, which will attempt to publish the event to Kafka once it becomes available again.
    public void publishEventFallback(String topic, WalletTransactionEvent event, Throwable ex) {
        log.warn("Circuit breaker triggered for Kafka publisher. Event will be retried via outbox pattern. " +
                "Transaction: {}, Wallet: {}", event.transactionId(), event.walletId(), ex);
        // Event is already in outbox, it will be retried by the outbox relay task
    }
}

