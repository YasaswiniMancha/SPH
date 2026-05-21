package com.wallet.smart.pay.hub.sph.event;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.wallet.smart.pay.hub.sph.resilience.ResilientKafkaPublisher;

@Component
@RequiredArgsConstructor
public class KafkaWalletEventPublisher implements WalletEventPublisher {

    private final KafkaTemplate<String, WalletTransactionEvent> kafkaTemplate;
    private final ResilientKafkaPublisher resilientKafkaPublisher;
    
    @Value("${wallet.events.topic}")
    private String topic;

    @Override
    @Async("walletEventExecutor")
    public void publishTransaction(WalletTransactionEvent event) {
        resilientKafkaPublisher.publishTransactionEvent(topic, event);
    }
}
