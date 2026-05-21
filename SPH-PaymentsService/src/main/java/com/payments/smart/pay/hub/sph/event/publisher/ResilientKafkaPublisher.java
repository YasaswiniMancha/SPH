package com.payments.smart.pay.hub.sph.event.publisher;

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

import com.payments.smart.pay.hub.sph.event.PaymentTransactionEvent;
import com.payments.smart.pay.hub.sph.exceptions.PaymentServiceException;

import java.util.concurrent.CompletableFuture;

/**
 * Resilient Kafka event publisher with circuit breaker and retry mechanisms.
 * Prevents cascading failures when Kafka is unavailable.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientKafkaPublisher {

	private final KafkaTemplate<String, PaymentTransactionEvent> kafkaTemplate;
	private static final long SEND_TIMEOUT_MS = 5000;

	@CircuitBreaker(name = "kafkaPublisher", fallbackMethod = "publishEventFallback")
	@TimeLimiter(name = "kafkaPublisher")
	@Retry(name = "kafkaPublisher")
	@Timed(value = "payment.kafka.publish", description = "Time taken to publish payment transaction event to Kafka")
	public CompletableFuture<Void> publishTransactionEvent(String topic, PaymentTransactionEvent event) {
		try {
			log.debug("Publishing payment transaction event to Kafka topic: {} for transaction: {}",
					topic, event.transactionId());

			Message<PaymentTransactionEvent> message = MessageBuilder
					.withPayload(event)
					.setHeader(KafkaHeaders.TOPIC, topic)
					.setHeader(KafkaHeaders.KEY, event.paymentId().toString())
					.setHeader("transaction-id", event.transactionId().toString())
					.setHeader("timestamp", System.currentTimeMillis())
					.build();
			return kafkaTemplate.send(message).thenAccept(result ->
					log.info("Kafka event published successfully: {}",
							event.transactionId()));

		} catch (Exception ex) {
			log.error("Failed to publish Kafka event: {}. Error: {}", event.transactionId(), ex.getMessage(), ex);
			throw new PaymentServiceException("Failed to publish Kafka event: " + event.transactionId(), ex);
		}
	}

	// Fallback method for circuit breaker when Kafka is unavailable
	public void publishEventFallback(String topic, PaymentTransactionEvent event, Throwable ex) {
		log.warn("Circuit breaker triggered for Kafka publisher. Event will be retried via outbox pattern. " +
				"Transaction: {}, Payment: {}", event.transactionId(), event.paymentId(), ex);
		// Event is already in outbox, it will be retried by the outbox relay task
	}
}