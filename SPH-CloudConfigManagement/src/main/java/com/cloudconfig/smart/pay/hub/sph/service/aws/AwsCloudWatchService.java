package com.cloudconfig.smart.pay.hub.sph.service.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import io.micrometer.core.annotation.Timed;

import java.time.Instant;

@Slf4j
@Service
public class AwsCloudWatchService {

    private final CloudWatchClient cloudWatchClient;
    
    @Value("${aws.cloudwatch.namespace}")
    private String namespace;

    public AwsCloudWatchService(CloudWatchClient cloudWatchClient) {
        this.cloudWatchClient = cloudWatchClient;
    }

    @Timed(value = "config.cloudwatch.metric", description = "CloudWatch metric publish time")
    public void recordConfigUpdate(String serviceName, String environment) {
        try {
            MetricDatum metricDatum = MetricDatum.builder()
                .metricName("ConfigUpdates")
                .value(1.0)
                .unit(StandardUnit.COUNT)
                .timestamp(Instant.now())
                .dimensions(
                    Dimension.builder().name("ServiceName").value(serviceName).build(),
                    Dimension.builder().name("Environment").value(environment).build()
                )
                .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                .namespace(namespace)
                .metricData(metricDatum)
                .build();

            cloudWatchClient.putMetricData(request);
            log.debug("CloudWatch metric published: ConfigUpdates for {}/{}", serviceName, environment);
        } catch (CloudWatchException e) {
            log.warn("Failed to record CloudWatch metric", e);
        }
    }

    public void recordS3Operation(String operation, long durationMs, String status) {
        try {
            MetricDatum metricDatum = MetricDatum.builder()
                .metricName("S3OperationDuration")
                .value((double) durationMs)
                .unit(StandardUnit.MILLISECONDS)
                .timestamp(Instant.now())
                .dimensions(
                    Dimension.builder().name("Operation").value(operation).build(),
                    Dimension.builder().name("Status").value(status).build()
                )
                .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                .namespace(namespace)
                .metricData(metricDatum)
                .build();

            cloudWatchClient.putMetricData(request);
            log.debug("S3 operation metric recorded: {} - {}", operation, status);
        } catch (CloudWatchException e) {
            log.warn("Failed to record S3 operation metric", e);
        }
    }

    public void recordValidationError(String serviceName, String environment) {
        try {
            MetricDatum metricDatum = MetricDatum.builder()
                .metricName("ConfigValidationErrors")
                .value(1.0)
                .unit(StandardUnit.COUNT)
                .timestamp(Instant.now())
                .dimensions(
                    Dimension.builder().name("ServiceName").value(serviceName).build(),
                    Dimension.builder().name("Environment").value(environment).build()
                )
                .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                .namespace(namespace)
                .metricData(metricDatum)
                .build();

            cloudWatchClient.putMetricData(request);
            log.debug("Validation error metric recorded for {}/{}", serviceName, environment);
        } catch (CloudWatchException e) {
            log.warn("Failed to record validation error metric", e);
        }
    }
}