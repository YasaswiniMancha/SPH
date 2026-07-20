package com.ledger.smart.pay.hub.sph.kafka;

import com.ledger.smart.pay.hub.sph.entity.Transaction;
import com.ledger.smart.pay.hub.sph.kafka.event.TransactionEvent;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "transaction-events";

    @Async("transactionExecutor")
    @Timed(value = "event.publish.completed", description = "Publish completed event")
    public void publishTransactionCompleted(Transaction transaction) {
        TransactionEvent event = new TransactionEvent(
            transaction.getId(),
            transaction.getUserId(),
            "COMPLETED",
            transaction.getAmount(),
            Instant.now()
        );
        kafkaTemplate.send(TOPIC, event);
        log.info("Transaction completed event published: {}", transaction.getId());
    }

    @Async("transactionExecutor")
    @Timed(value = "event.publish.failed", description = "Publish failed event")
    public void publishTransactionFailed(Transaction transaction) {
        TransactionEvent event = new TransactionEvent(
            transaction.getId(),
            transaction.getUserId(),
            "FAILED",
            transaction.getAmount(),
            Instant.now()
        );
        kafkaTemplate.send(TOPIC, event);
        log.warn("Transaction failed event published: {}", transaction.getId());
    }

    @Async("transactionExecutor")
    @Timed(value = "event.publish.reversed", description = "Publish reversed event")
    public void publishTransactionReversed(Transaction transaction) {
        TransactionEvent event = new TransactionEvent(
            transaction.getId(),
            transaction.getUserId(),
            "REVERSED",
            transaction.getAmount(),
            Instant.now()
        );
        kafkaTemplate.send(TOPIC, event);
        log.info("Transaction reversed event published: {}", transaction.getId());
    }
}