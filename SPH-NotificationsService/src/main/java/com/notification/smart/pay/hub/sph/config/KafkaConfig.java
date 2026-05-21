package com.notification.smart.pay.hub.sph.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultAfterRollbackProcessor;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public DefaultAfterRollbackProcessor afterRollbackProcessor() {
        return new DefaultAfterRollbackProcessor();
    }
}