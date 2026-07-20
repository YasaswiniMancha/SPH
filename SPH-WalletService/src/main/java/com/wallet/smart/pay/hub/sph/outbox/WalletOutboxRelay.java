package com.wallet.smart.pay.hub.sph.outbox;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.smart.pay.hub.sph.event.WalletEventPublisher;
import com.wallet.smart.pay.hub.sph.event.WalletTransactionEvent;
import com.wallet.smart.pay.hub.sph.repository.WalletOutboxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

//this class is used to relay the pending events to the event bus
@Component
public class WalletOutboxRelay {

    private final WalletOutboxEventRepository outboxEventRepository;
    private final WalletOutboxService outboxService;
    private final WalletEventPublisher walletEventPublisher;
    private final ObjectMapper objectMapper;

    public WalletOutboxRelay(
        WalletOutboxEventRepository outboxEventRepository,
        WalletOutboxService outboxService,
        WalletEventPublisher walletEventPublisher,
        ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxService = outboxService;
        this.walletEventPublisher = walletEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${wallet.outbox.relay-delay-ms:1500}")
    public void relayPendingEvents() {
        List<WalletOutboxEvent> pendingEvents =
            outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (WalletOutboxEvent outboxEvent : pendingEvents) {
            try {
                WalletTransactionEvent event =
                    objectMapper.readValue(outboxEvent.getPayload(), WalletTransactionEvent.class);
                walletEventPublisher.publishTransaction(event);
                outboxService.markPublished(outboxEvent);
            } catch (Exception ex) {
                outboxService.markFailed(outboxEvent, ex.getMessage());
            }
        }
    }
}
