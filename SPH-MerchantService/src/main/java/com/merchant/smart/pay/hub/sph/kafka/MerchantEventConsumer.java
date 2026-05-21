package com.merchant.smart.pay.hub.sph.kafka;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MerchantEventConsumer {

    /**
     * Listen to merchant events from other services
     * Virtual threads for efficient concurrency (Java 21+)
     */
    @Timed(value = "kafka.merchant.events.consume", description = "Merchant events consumption time")
    @KafkaListener(
        topics = "merchant-events",
        groupId = "merchant-consumer-group",
        concurrency = "10",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMerchantEvents(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("Received message from topic: {} partition: {} offset: {}", topic, partition, offset);
            
            // Parse and process event asynchronously using virtual threads
            processEventAsync(message)
                .thenAccept(result -> {
                    log.info("Event processed successfully: {}", result);
                    // Manual commit after successful processing
                    if (acknowledgment != null) {
                        acknowledgment.acknowledge();
                    }
                })
                .exceptionally(ex -> {
                    log.error("Failed to process event", ex);
                    return null;
                });

        } catch (Exception e) {
            log.error("Error consuming merchant event: {}", message, e);
        }
    }

    /**
     * Async event processing using CompletableFuture
     */
    private CompletableFuture<String> processEventAsync(String message) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Processing event: {}", message);
            // Parse and handle event
            String[] parts = message.split(",");
            return "Processed: " + parts[0];
        });
    }

    /**
     * High-throughput batch consumer
     */
    @KafkaListener(
        topics = "merchant-batch-events",
        groupId = "merchant-batch-group",
        concurrency = "5",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMerchantBatchEvents(
            @Payload String message,
            Acknowledgment acknowledgment) {
        try {
            log.info("Processing batch event: {}", message);
            // Batch processing logic here
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            log.error("Error processing batch event", e);
        }
    }
}