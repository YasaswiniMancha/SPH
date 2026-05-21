package com.payments.smart.pay.hub.sph.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.smart.pay.hub.sph.event.PaymentTransactionEvent;
import com.payments.smart.pay.hub.sph.outbox.OutboxStatus;
import com.payments.smart.pay.hub.sph.outbox.PaymentOutboxEvent;
import com.payments.smart.pay.hub.sph.repository.PaymentOutboxEventRepository;

/**
 * Service for managing payment outbox events.
 * Implements the transactional outbox pattern for guaranteed event delivery.
 */
@Service
public class PaymentOutboxService {

	private final PaymentOutboxEventRepository outboxEventRepository;
	private final ObjectMapper objectMapper;

	public PaymentOutboxService(PaymentOutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
		this.outboxEventRepository = outboxEventRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public void enqueueTransactionEvent(PaymentTransactionEvent event) {
		PaymentOutboxEvent outboxEvent = new PaymentOutboxEvent();
		outboxEvent.setAggregateType("PAYMENT");
		outboxEvent.setAggregateId(event.paymentId().toString());
		outboxEvent.setEventType(event.type());
		outboxEvent.setPayload(toJson(event));
		outboxEvent.setStatus(OutboxStatus.PENDING);
		outboxEvent.setRetryCount(0);
		outboxEventRepository.save(outboxEvent);
	}

	@Transactional
	public void markPublished(PaymentOutboxEvent event) {
		event.setStatus(OutboxStatus.PUBLISHED);
		event.setPublishedAt(Instant.now());
		event.setLastError(null);
		outboxEventRepository.save(event);
	}

	@Transactional
	public void markFailed(PaymentOutboxEvent event, String errorMessage) {
		event.setStatus(OutboxStatus.FAILED);
		event.setRetryCount(event.getRetryCount() + 1);
		event.setLastError(trimError(errorMessage));
		outboxEventRepository.save(event);
	}

	private String toJson(PaymentTransactionEvent event) {
		try {
			return objectMapper.writeValueAsString(event);
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to serialize payment event", ex);
		}
	}

	private String trimError(String errorMessage) {
		if (errorMessage == null) {
			return "Unknown error";
		}
		return errorMessage.length() > 480 ? errorMessage.substring(0, 480) : errorMessage;
	}
}