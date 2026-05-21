package com.payments.smart.pay.hub.sph.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.payments.smart.pay.hub.sph.event.publisher.PaymentEventPublisher;
import com.payments.smart.pay.hub.sph.event.publisher.ResilientKafkaPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * Kafka implementation of PaymentEventPublisher.
 * Publishes payment transaction events to Kafka topics asynchronously.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

	private final ResilientKafkaPublisher resilientKafkaPublisher;
	
	@Value("${payment.events.topic:payment.transactions.v1}")
	private String topic;

	@Override
	@Async("paymentEventExecutor")
	public void publishTransaction(PaymentTransactionEvent event) {
		log.debug("Publishing payment transaction event to Kafka. Event: {}", event.transactionId());
		resilientKafkaPublisher.publishTransactionEvent(topic, event);
	}
}