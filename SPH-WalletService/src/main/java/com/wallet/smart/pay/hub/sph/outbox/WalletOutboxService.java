package com.wallet.smart.pay.hub.sph.outbox;

import java.time.Instant;

import com.wallet.smart.pay.hub.sph.event.WalletTransactionEvent;
import com.wallet.smart.pay.hub.sph.repository.WalletOutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

//this class is used to enqueue transaction events to the outbox
//the outbox is used to store the transaction events that need to be published to the event bus
@Service
public class WalletOutboxService {

    private final WalletOutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public WalletOutboxService(WalletOutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enqueueTransactionEvent(WalletTransactionEvent event) {
        WalletOutboxEvent outboxEvent = new WalletOutboxEvent();
        outboxEvent.setAggregateType("WALLET");
        outboxEvent.setAggregateId(event.walletId().toString());
        outboxEvent.setEventType(event.type());
        outboxEvent.setPayload(toJson(event));
        outboxEvent.setStatus(OutboxStatus.PENDING);
        outboxEvent.setRetryCount(0);
        outboxEventRepository.save(outboxEvent);
    }

    @Transactional
    public void markPublished(WalletOutboxEvent event) {
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
        event.setLastError(null);
        outboxEventRepository.save(event);
    }

    @Transactional
    public void markFailed(WalletOutboxEvent event, String errorMessage) {
        event.setStatus(OutboxStatus.FAILED);
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastError(trimError(errorMessage));
        outboxEventRepository.save(event);
    }

    private String toJson(WalletTransactionEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize wallet event", ex);
        }
    }

    private String trimError(String errorMessage) {
        if (errorMessage == null) {
            return "Unknown error";
        }
        return errorMessage.length() > 480 ? errorMessage.substring(0, 480) : errorMessage;
    }
}
