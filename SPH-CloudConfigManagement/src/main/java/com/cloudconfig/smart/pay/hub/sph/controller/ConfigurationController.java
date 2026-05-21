package com.cloudconfig.smart.pay.hub.sph.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloudconfig.smart.pay.hub.sph.dto.ConfigUpdateRequest;
import com.cloudconfig.smart.pay.hub.sph.dto.ConfigUpdateResponse;
import com.cloudconfig.smart.pay.hub.sph.dto.ConfigurationDto;
import com.cloudconfig.smart.pay.hub.sph.entity.ConfigurationAuditEntity;
import com.cloudconfig.smart.pay.hub.sph.service.ConfigurationService;
import com.cloudconfig.smart.pay.hub.sph.service.aws.AwsCloudWatchService;
import com.cloudconfig.smart.pay.hub.sph.service.aws.AwsEc2Service;
import com.cloudconfig.smart.pay.hub.sph.service.aws.AwsRdsService;
import com.cloudconfig.smart.pay.hub.sph.service.aws.AwsS3ConfigService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/config")
@CrossOrigin(origins = "*")
public class ConfigurationController {

    private final ConfigurationService configService;
    private final AwsS3ConfigService s3Service;
    private final AwsCloudWatchService cloudWatchService;
    private final AwsEc2Service ec2Service;
    private final AwsRdsService rdsService;

    public ConfigurationController(
            ConfigurationService configService,
            AwsS3ConfigService s3Service,
            AwsCloudWatchService cloudWatchService,
            AwsEc2Service ec2Service,
            AwsRdsService rdsService) {
        this.configService = configService;
        this.s3Service = s3Service;
        this.cloudWatchService = cloudWatchService;
        this.ec2Service = ec2Service;
        this.rdsService = rdsService;
    }

    @GetMapping("/{serviceName}/{environment}/{configKey}")
    public ResponseEntity<ConfigurationDto> getConfiguration(
            @PathVariable String serviceName,
            @PathVariable String environment,
            @PathVariable String configKey) {
        log.info("Fetching config: {}/{}/{}", serviceName, environment, configKey);
        return ResponseEntity.ok(configService.getConfiguration(serviceName, environment, configKey));
    }

    @GetMapping("/{serviceName}/{environment}")
    public ResponseEntity<Page<ConfigurationDto>> getServiceConfigs(
            @PathVariable String serviceName,
            @PathVariable String environment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Fetching configs for service: {}/{}", serviceName, environment);
        return ResponseEntity.ok(configService.getServiceConfigs(
            serviceName, environment, PageRequest.of(page, size)));
    }

    @PostMapping
    public ResponseEntity<ConfigUpdateResponse> createOrUpdateConfiguration(
            @Valid @RequestBody ConfigUpdateRequest request,
            @RequestHeader("X-User-Id") String userId) {
        log.info("Creating/updating config: {}/{}/{} by user: {}", 
            request.getServiceName(), request.getEnvironment(), request.getConfigKey(), userId);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(configService.createOrUpdateConfiguration(request, userId));
    }

    @DeleteMapping("/{configId}")
    public ResponseEntity<Void> deleteConfiguration(
            @PathVariable String configId,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) String reason) {
        log.info("Deleting config: {} by user: {}", configId, userId);
        configService.deleteConfiguration(configId, userId, reason);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{configId}/audit")
    public ResponseEntity<Page<ConfigurationAuditEntity>> getAuditTrail(
            @PathVariable String configId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Fetching audit trail for config: {}", configId);
        return ResponseEntity.ok(configService.getAuditTrail(configId, PageRequest.of(page, size)));
    }

    // ==================== AWS Integration Endpoints ====================

    @GetMapping("/aws/ec2/instances")
    public ResponseEntity<?> getEC2Instances() {
        log.info("Fetching running EC2 instances");
        return ResponseEntity.ok(ec2Service.getRunningInstances());
    }

    @GetMapping("/aws/ec2/service/{serviceName}")
    public ResponseEntity<?> getServiceInstances(@PathVariable String serviceName) {
        log.info("Fetching EC2 instances for service: {}", serviceName);
        return ResponseEntity.ok(ec2Service.getInstancesByService(serviceName));
    }

    @GetMapping("/aws/ec2/{instanceId}")
    public ResponseEntity<?> getInstanceDetails(@PathVariable String instanceId) {
        log.info("Fetching EC2 instance details: {}", instanceId);
        return ResponseEntity.ok(ec2Service.getInstanceDetails(instanceId));
    }

    @GetMapping("/aws/rds/status")
    public ResponseEntity<?> getRdsStatus() {
        log.info("Fetching RDS instance status");
        return ResponseEntity.ok(rdsService.getInstanceDetails());
    }

    @GetMapping("/aws/rds/backups")
    public ResponseEntity<?> getRdsBackups() {
        log.info("Fetching RDS backups");
        return ResponseEntity.ok(rdsService.listBackups());
    }

    @PostMapping("/aws/rds/backup")
    public ResponseEntity<?> createRdsBackup(@RequestParam String backupId) {
        log.info("Creating RDS backup: {}", backupId);
        return ResponseEntity.ok(java.util.Map.of(
            "backupId", rdsService.createBackup(backupId),
            "status", "BACKUP_INITIATED",
            "timestamp", System.currentTimeMillis()
        ));
    }

    @PutMapping("/aws/rds/modify")
    public ResponseEntity<?> modifyRdsInstance(@RequestParam String instanceClass) {
        log.info("Modifying RDS instance class: {}", instanceClass);
        rdsService.modifyInstance(instanceClass);
        return ResponseEntity.ok(java.util.Map.of(
            "status", "MODIFICATION_REQUESTED",
            "instanceClass", instanceClass
        ));
    }

    @PostMapping("/aws/ec2/{instanceId}/tag")
    public ResponseEntity<?> tagInstance(
            @PathVariable String instanceId,
            @RequestParam String tagKey,
            @RequestParam String tagValue) {
        log.info("Tagging EC2 instance: {} with {}={}", instanceId, tagKey, tagValue);
        ec2Service.tagInstance(instanceId, tagKey, tagValue);
        return ResponseEntity.ok(java.util.Map.of(
            "instanceId", instanceId,
            "tagKey", tagKey,
            "tagValue", tagValue,
            "status", "TAGGED"
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(java.util.Map.of(
            "status", "UP",
            "service", "SPH-CloudConfigServer",
            "timestamp", System.currentTimeMillis()
        ));
    }
}