package com.cloudconfig.smart.pay.hub.sph.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.cloudconfig.smart.pay.hub.sph.service.aws.*;
import io.micrometer.core.annotation.Timed;

import java.time.Instant;

@Slf4j
@Service
public class ConfigUpdateNotificationConsumer {

    private final AwsCloudWatchService cloudWatchService;
    private final AwsEc2Service ec2Service;

    public ConfigUpdateNotificationConsumer(AwsCloudWatchService cloudWatchService, 
                                           AwsEc2Service ec2Service) {
        this.cloudWatchService = cloudWatchService;
        this.ec2Service = ec2Service;
    }

    /**
     * Listen to config update events from config-updates topic
     * Real-time processing with millisecond latency
     */
    @Timed(value = "config.kafka.listener", description = "Kafka listener processing time")
    @KafkaListener(
        topics = "config-updates",
        groupId = "config-notification-group",
        concurrency = "5",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void processConfigUpdate(String message) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("Processing config update: {}", message);
            
            // Parse message: serviceName=X,environment=Y,configKey=Z,action=ACTION
            String[] parts = message.split(",");
            String serviceName = extractValue(parts, "serviceName");
            String environment = extractValue(parts, "environment");
            String configKey = extractValue(parts, "configKey");
            String action = extractValue(parts, "action");
            
            // Publish CloudWatch metric immediately
            cloudWatchService.recordConfigUpdate(serviceName, environment);
            
            // Get running instances and trigger config reload
            var instances = ec2Service.getInstancesByService(serviceName);
            log.info("Found {} instances for service: {}", instances.size(), serviceName);
            
            // Could trigger Instance Refresh in Auto-scaling Groups here
            triggerInstanceRefresh(serviceName, environment);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Config update notification processed in {} ms", duration);
            
        } catch (Exception e) {
            log.error("Failed to process config update: {}", message, e);
            // Send to DLQ (Dead Letter Queue) - defined in Kafka topic
        }
    }

    /**
     * Batch listener for high-throughput scenarios
     */
    @KafkaListener(
        topics = "config-updates-batch",
        groupId = "config-notification-batch-group",
        concurrency = "3",
        containerFactory = "kafkaBatchListenerContainerFactory"
    )
    public void processConfigUpdateBatch(java.util.List<String> messages) {
        log.info("Processing batch of {} config updates", messages.size());
        
        messages.parallelStream().forEach(message -> {
            try {
                processConfigUpdate(message);
            } catch (Exception e) {
                log.error("Failed to process message in batch: {}", message, e);
            }
        });
    }

    private String extractValue(String[] parts, String key) {
        for (String part : parts) {
            if (part.startsWith(key + "=")) {
                return part.substring((key + "=").length());
            }
        }
        return "";
    }

    private void triggerInstanceRefresh(String serviceName, String environment) {
        try {
            // This would integrate with AWS Auto Scaling Groups
            // For now, just log it
            log.info("[TODO] Trigger instance refresh for {}/{}", serviceName, environment);
        } catch (Exception e) {
            log.warn("Failed to trigger instance refresh", e);
        }
    }
}