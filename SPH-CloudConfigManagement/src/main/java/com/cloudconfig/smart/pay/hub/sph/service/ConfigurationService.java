package com.cloudconfig.smart.pay.hub.sph.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudconfig.smart.pay.hub.sph.dto.ConfigUpdateRequest;
import com.cloudconfig.smart.pay.hub.sph.dto.ConfigUpdateResponse;
import com.cloudconfig.smart.pay.hub.sph.dto.ConfigurationDto;
import com.cloudconfig.smart.pay.hub.sph.entity.ConfigurationAuditEntity;
import com.cloudconfig.smart.pay.hub.sph.entity.ConfigurationEntity;
import com.cloudconfig.smart.pay.hub.sph.event.ConfigNotificationProducer;
import com.cloudconfig.smart.pay.hub.sph.repository.ConfigurationAuditRepository;
import com.cloudconfig.smart.pay.hub.sph.repository.ConfigurationRepository;
import com.cloudconfig.smart.pay.hub.sph.service.aws.AwsCloudWatchService;
import com.cloudconfig.smart.pay.hub.sph.service.aws.AwsEc2Service;
import com.cloudconfig.smart.pay.hub.sph.service.aws.AwsLambdaService;
import com.cloudconfig.smart.pay.hub.sph.service.aws.AwsParameterStoreService;
import com.cloudconfig.smart.pay.hub.sph.service.aws.AwsS3ConfigService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class ConfigurationService {

    private final ConfigurationRepository configRepository;
    private final ConfigurationAuditRepository auditRepository;
    private final AwsS3ConfigService s3Service;
    private final AwsCloudWatchService cloudWatchService;
    private final AwsLambdaService lambdaService;
    private final AwsEc2Service ec2Service;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AwsParameterStoreService parameterStoreService;
    private final ConfigNotificationProducer notificationProducer;

    @Cacheable(value = "configs", key = "#serviceName + ':' + #environment + ':' + #configKey")
    @Timed(value = "config.get", description = "Get configuration time")
    public ConfigurationDto getConfiguration(String serviceName, String environment, String configKey) {
        return configRepository.findByServiceNameAndEnvironmentAndConfigKey(
            serviceName, environment, configKey)
            .map(this::toDto)
            .orElseThrow(() -> new RuntimeException("Configuration not found: " + configKey));
    }

    @Timed(value = "config.list", description = "List configurations time")
    public Page<ConfigurationDto> getServiceConfigs(String serviceName, String environment, Pageable pageable) {
        return configRepository.findByServiceNameAndEnvironment(serviceName, environment, pageable)
            .map(this::toDto);
    }

    @CacheEvict(value = "configs", allEntries = true)
    @Timed(value = "config.create.update", description = "Create/update configuration time")
    @CircuitBreaker(name = "configServiceCB", fallbackMethod = "createConfigFallback")
    public ConfigUpdateResponse createOrUpdateConfiguration(ConfigUpdateRequest request, String userId) {
        long startTime = System.currentTimeMillis();

        try {
            // Validate configuration
            boolean isValid = lambdaService.validateConfigWithLambda(
                request.getServiceName(), 
                request.getConfigValue()
            );
            
            if (!isValid) {
                cloudWatchService.recordValidationError(
                    request.getServiceName(), 
                    request.getEnvironment()
                );
                throw new RuntimeException("Configuration validation failed");
            }

            ConfigurationEntity config = configRepository.findByServiceNameAndEnvironmentAndConfigKey(
                request.getServiceName(), 
                request.getEnvironment(), 
                request.getConfigKey()
            )
            .orElseGet(() -> ConfigurationEntity.builder()
                .serviceName(request.getServiceName())
                .environment(request.getEnvironment())
                .configKey(request.getConfigKey())
                .build()
            );

            String previousValue = config.getConfigValue();
            config.setConfigValue(request.getConfigValue());
            config.setDescription(request.getDescription());
            config.setIsEncrypted(request.getIsEncrypted() != null && request.getIsEncrypted());

            // Save to database
            ConfigurationEntity saved = configRepository.save(config);

            // Upload to S3
            String s3Key = s3Service.uploadConfig(
                request.getServiceName(),
                request.getEnvironment(),
                request.getConfigKey(),
                request.getConfigValue(),
                saved.getVersion()
            );
            saved.setS3ObjectKey(s3Key);
            configRepository.save(saved);

            // Record audit trail
            auditRepository.save(ConfigurationAuditEntity.builder()
                .configId(saved.getId())
                .action(previousValue == null ? "CREATE" : "UPDATE")
                .previousValue(previousValue)
                .newValue(request.getConfigValue())
                .modifiedBy(userId)
                .changeReason(request.getChangeReason())
                .build()
            );

            // Publish to CloudWatch
            cloudWatchService.recordConfigUpdate(request.getServiceName(), request.getEnvironment());

            // Trigger Lambda notification async
            notifyServicesAsync(request.getServiceName(), request.getEnvironment(), request.getConfigKey(), request.getConfigValue());

            // Publish to Kafka for other services
            publishConfigUpdateEvent(request, previousValue == null ? "CREATE" : "UPDATE");

            long duration = System.currentTimeMillis() - startTime;
            cloudWatchService.recordS3Operation("CONFIG_UPDATE", duration, "SUCCESS");

            return ConfigUpdateResponse.builder()
                .id(saved.getId())
                .message("Configuration saved successfully")
                .s3ObjectKey(s3Key)
                .configKey(request.getConfigKey())
                .timestamp(System.currentTimeMillis())
                .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            cloudWatchService.recordS3Operation("CONFIG_UPDATE", duration, "FAILED");
            log.error("Failed to create/update configuration", e);
            throw e;
        }
    }

    public ConfigUpdateResponse createConfigFallback(ConfigUpdateRequest request, String userId, Exception ex) {
        log.error("Config creation fallback triggered: {}", ex.getMessage());
        throw new RuntimeException("Config service temporarily unavailable. Please try again later.");
    }

    @CacheEvict(value = "configs", allEntries = true)
    @Timed(value = "config.delete", description = "Delete configuration time")
    public void deleteConfiguration(String configId, String userId, String reason) {
        ConfigurationEntity config = configRepository.findById(configId)
            .orElseThrow(() -> new RuntimeException("Configuration not found"));

        // Record audit
        auditRepository.save(ConfigurationAuditEntity.builder()
            .configId(configId)
            .action("DELETE")
            .previousValue(config.getConfigValue())
            .newValue(null)
            .modifiedBy(userId)
            .changeReason(reason)
            .build()
        );

        // Delete from S3
        if (config.getS3ObjectKey() != null) {
            s3Service.deleteConfig(config.getS3ObjectKey());
        }

        // Soft delete from database
        config.setIsActive(false);
        configRepository.save(config);

        log.info("Configuration deleted: {}", configId);
    }

    @Timed(value = "config.audit.list", description = "List audit trail time")
    public Page<ConfigurationAuditEntity> getAuditTrail(String configId, Pageable pageable) {
        return auditRepository.findByConfigId(configId, pageable);
    }

    @Async("configExecutor")
    private void notifyServicesAsync(String serviceName, String environment, String configKey, String configValue) {
        try {
            parameterStoreService.putParameter(serviceName, environment, configKey, configValue);
            notificationProducer.publishConfigUpdate(serviceName, environment, configKey);
        } catch (Exception e) {
            log.warn("Failed to notify services asynchronously", e);
        }
    }

    private void publishConfigUpdateEvent(ConfigUpdateRequest request, String action) {
        try {
            String message = String.format(
                "serviceName=%s,environment=%s,configKey=%s,action=%s,timestamp=%d",
                request.getServiceName(),
                request.getEnvironment(),
                request.getConfigKey(),
                action,
                System.currentTimeMillis()
            );
            kafkaTemplate.send("config-updates", message);
            log.debug("Config update event published to Kafka");
        } catch (Exception e) {
            log.warn("Failed to publish config update event to Kafka", e);
        }
    }

    private ConfigurationDto toDto(ConfigurationEntity entity) {
        return ConfigurationDto.builder()
            .id(entity.getId())
            .serviceName(entity.getServiceName())
            .environment(entity.getEnvironment())
            .configKey(entity.getConfigKey())
            .configValue(entity.getConfigValue())
            .description(entity.getDescription())
            .isEncrypted(entity.getIsEncrypted())
            .isActive(entity.getIsActive())
            .version(entity.getVersion())
            .build();
    }
}