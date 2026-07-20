package com.wallet.smart.pay.hub.sph.outbox;

// ...existing code...
import com.wallet.smart.pay.hub.sph.repository.WalletOutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;

@Component
public class OutboxPublisher {

    private final WalletOutboxEventRepository repository;
    private final WalletOutboxService outboxService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(WalletOutboxEventRepository repository,
                           WalletOutboxService outboxService,
                           KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.outboxService = outboxService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.delay:5000}")
    public void publishPending() {
        // Use var (Java 10+) and records for lightweight in-memory DTOs (Java 17)
        var pending = repository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        // Process each event asynchronously with non-blocking Kafka send + callback
        pending.forEach(ev -> {
            try {
                // Create a lightweight DTO (record) for in-memory processing
                // WalletOutboxEvent does not expose createdAt getter in the entity, so pass null for createdAt
                var dto = new OutboxEventDto(ev.getId(), ev.getAggregateType(), ev.getEventType(), ev.getAggregateId(), ev.getPayload(), ev.getStatus(), null, ev.getPublishedAt(), ev.getRetryCount(), ev.getLastError());

                var message = new KafkaMessage("wallet.transactions", dto.aggregateId(), dto.payload());

                var future = kafkaTemplate.send(message.topic(), message.key(), message.payload());

                // Use CompletableFuture.whenComplete when available; otherwise fallback to running get() in another thread
                if (future instanceof CompletableFuture<?> cf) {
                    cf.whenComplete((res, ex) -> {
                        if (ex == null) {
                            try {
                                outboxService.markPublished(ev);
                            } catch (Exception e) {
                                outboxService.markFailed(ev, e.getMessage());
                            }
                        } else {
                            outboxService.markFailed(ev, ex.getMessage());
                        }
                    });
                } else {
                    CompletableFuture.runAsync(() -> {
                        try {
                            future.get();
                            outboxService.markPublished(ev);
                        } catch (Exception ex) {
                            outboxService.markFailed(ev, ex.getMessage());
                        }
                    });
                }

            } catch (Exception ex) {
                outboxService.markFailed(ev, ex.getMessage());
            }
        });
    }
}

