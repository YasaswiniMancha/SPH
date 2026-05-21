package com.payments.smart.pay.hub.sph.event.publisher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.payments.smart.pay.hub.sph.event.PaymentTransactionEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

	private final KafkaTemplate<String, PaymentTransactionEvent> kafkaTemplate;
	private final ResilientKafkaPublisher resilientKafkaPublisher;
	
	@Value("${payment.events.topic}")
	private String topic;

	@Override
	@Async("paymentEventExecutor")
	public void publishTransaction(PaymentTransactionEvent event) {
		resilientKafkaPublisher.publishTransactionEvent(topic, event);
	}
}