package com.payments.smart.pay.hub.sph.outbox;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.smart.pay.hub.sph.event.PaymentTransactionEvent;
import com.payments.smart.pay.hub.sph.event.publisher.PaymentEventPublisher;
import com.payments.smart.pay.hub.sph.repository.PaymentOutboxEventRepository;
import com.payments.smart.pay.hub.sph.service.PaymentOutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled task that relays pending outbox events to Kafka.
 * Implements the transactional outbox pattern for guaranteed event delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentOutboxRelay {

	private final PaymentOutboxEventRepository outboxEventRepository;
	private final PaymentOutboxService outboxService;
	private final PaymentEventPublisher paymentEventPublisher;
	private final ObjectMapper objectMapper;

	@Scheduled(fixedDelayString = "${payment.outbox.relay-delay-ms:1500}")
	public void relayPendingEvents() {
		try {
			List<PaymentOutboxEvent> pendingEvents =
				outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
			
			if (pendingEvents.isEmpty()) {
				log.debug("No pending outbox events to relay");
				return;
			}
			
			log.debug("Found {} pending outbox events to relay", pendingEvents.size());
			
			for (PaymentOutboxEvent outboxEvent : pendingEvents) {
				try {
					PaymentTransactionEvent event =
						objectMapper.readValue(outboxEvent.getPayload(), PaymentTransactionEvent.class);
					paymentEventPublisher.publishTransaction(event);
					outboxService.markPublished(outboxEvent);
					log.debug("Successfully relayed outbox event: {}", outboxEvent.getId());
				} catch (Exception ex) {
					outboxService.markFailed(outboxEvent, ex.getMessage());
					log.error("Failed to relay outbox event: {}", outboxEvent.getId(), ex);
				}
			}
		} catch (Exception ex) {
			log.error("Error during outbox relay execution", ex);
		}
	}
}