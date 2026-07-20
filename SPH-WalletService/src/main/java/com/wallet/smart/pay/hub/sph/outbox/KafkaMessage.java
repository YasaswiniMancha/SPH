package com.wallet.smart.pay.hub.sph.outbox;

/**
 * Small immutable carrier for Kafka messages using Java 17 record.
 */
public record KafkaMessage(String topic, String key, String payload) {
}

