package com.cloudconfig.smart.pay.hub.sph.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

@Slf4j
@Service
public class ConfigNotificationProducer {

    private final SnsClient snsClient;
    
    @Value("${aws.sns.config-update-topic:}")
    private String configUpdateTopicArn;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    public ConfigNotificationProducer(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    @Timed(value = "config.sns.publish", description = "SNS publish time")
    public void publishConfigUpdateNotification(String serviceName, String environment, 
                                               String configKey, String action) {
        if (configUpdateTopicArn == null || configUpdateTopicArn.isEmpty()) {
            log.debug("SNS topic not configured, skipping notification");
            return;
        }

        try {
            String message = String.format(
                "Service: %s | Environment: %s | Config: %s | Action: %s | Time: %d",
                serviceName, environment, configKey, action, System.currentTimeMillis()
            );

            PublishRequest request = PublishRequest.builder()
                .topicArn(configUpdateTopicArn)
                .message(message)
                .subject(String.format("SmartPayHub Config Update: %s", serviceName))
                .messageAttributes(
                    java.util.Map.of(
                        "serviceName", MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(serviceName)
                            .build(),
                        "environment", MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(environment)
                            .build()
                    )
                )
                .build();

            PublishResponse result = snsClient.publish(request);
            log.info("Config update notification published to SNS: {}", result.messageId());

        } catch (SnsException e) {
            log.error("Failed to publish SNS notification", e);
            // Don't throw - config update should proceed even if notification fails
        }
    }

    public void publishConfigUpdate(String serviceName, String environment, String configKey) {
        publishConfigUpdateNotification(serviceName, environment, configKey, "UPDATE");
    }

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }
}