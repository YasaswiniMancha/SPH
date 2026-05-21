package com.cloudconfig.smart.pay.hub.sph.service.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import io.micrometer.core.annotation.Timed;

import java.util.List;

@Slf4j
@Service
public class AwsEc2Service {

    private final Ec2Client ec2Client;
    
    @Value("${aws.ec2.tag.environment}")
    private String environment;

    public AwsEc2Service(Ec2Client ec2Client) {
        this.ec2Client = ec2Client;
    }

    @Timed(value = "config.ec2.describe", description = "EC2 describe instances time")
    public List<Instance> getRunningInstances() {
        try {
            Filter envFilter = Filter.builder()
                .name("tag:Environment")
                .values(environment)
                .build();

            Filter stateFilter = Filter.builder()
                .name("instance-state-name")
                .values("running")
                .build();

            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                .filters(envFilter, stateFilter)
                .build();

            DescribeInstancesResponse response = ec2Client.describeInstances(request);

            List<Instance> instances = response.reservations().stream()
                .flatMap(r -> r.instances().stream())
                .toList();
            
            log.info("Found {} running EC2 instances for environment: {}", instances.size(), environment);
            return instances;
        } catch (Ec2Exception e) {
            log.error("Failed to get EC2 instances", e);
            throw new RuntimeException("EC2 describe failed", e);
        }
    }

    public Instance getInstanceDetails(String instanceId) {
        try {
            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();

            DescribeInstancesResponse response = ec2Client.describeInstances(request);
            
            return response.reservations().stream()
                .flatMap(r -> r.instances().stream())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Instance not found: " + instanceId));
        } catch (Ec2Exception e) {
            log.error("Failed to get EC2 instance details for: {}", instanceId, e);
            throw new RuntimeException("EC2 describe failed", e);
        }
    }

    public List<Instance> getInstancesByService(String serviceName) {
        try {
            Filter serviceFilter = Filter.builder()
                .name("tag:Service")
                .values(serviceName)
                .build();

            Filter stateFilter = Filter.builder()
                .name("instance-state-name")
                .values("running")
                .build();

            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                .filters(serviceFilter, stateFilter)
                .build();

            DescribeInstancesResponse response = ec2Client.describeInstances(request);

            List<Instance> instances = response.reservations().stream()
                .flatMap(r -> r.instances().stream())
                .toList();
            
            log.info("Found {} EC2 instances for service: {}", instances.size(), serviceName);
            return instances;
        } catch (Ec2Exception e) {
            log.error("Failed to get service instances for: {}", serviceName, e);
            throw new RuntimeException("EC2 describe failed", e);
        }
    }

    public void tagInstance(String instanceId, String tagKey, String tagValue) {
        try {
            Tag tag = Tag.builder()
                .key(tagKey)
                .value(tagValue)
                .build();

            CreateTagsRequest request = CreateTagsRequest.builder()
                .resources(instanceId)
                .tags(tag)
                .build();

            ec2Client.createTags(request);
            log.info("Tagged EC2 instance {} with {}={}", instanceId, tagKey, tagValue);
        } catch (Ec2Exception e) {
            log.error("Failed to tag EC2 instance: {}", instanceId, e);
            throw new RuntimeException("EC2 tag failed", e);
        }
    }
}