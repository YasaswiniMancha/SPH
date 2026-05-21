package com.cloudconfig.smart.pay.hub.sph.service.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * AWS Lambda Function for Config Update Notifications
 * Deploy separately as Lambda function, NOT as part of Spring Boot microservice
 * 
 * This is optional - use only if deploying Lambda separately
 */
public class ConfigUpdateNotifierLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final CloudWatchClient cloudWatchClient = CloudWatchClient.builder().build();
    private static final SnsClient snsClient = SnsClient.builder().build();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Received event: " + event);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String action = (String) event.get("action");
            
            if ("CONFIG_UPDATED".equals(action)) {
                return handleConfigUpdate(event, context);
            } else if ("VALIDATE_CONFIG".equals(action)) {
                return handleConfigValidation(event, context);
            } else {
                context.getLogger().log("Unknown action: " + action);
                response.put("statusCode", 400);
                response.put("body", "Unknown action");
                return response;
            }
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            response.put("statusCode", 500);
            response.put("body", "Internal server error");
            return response;
        }
    }

    private Map<String, Object> handleConfigUpdate(Map<String, Object> event, Context context) {
        String serviceName = (String) event.get("serviceName");
        String environment = (String) event.get("environment");
        String configKey = (String) event.get("configKey");
        Long timestamp = (Long) event.get("timestamp");

        context.getLogger().log(String.format(
            "Config updated: %s/%s/%s at %d",
            serviceName, environment, configKey, timestamp
        ));

        // Record CloudWatch metric
        recordMetric(serviceName, environment, "ConfigUpdates");

        // Send SNS notification to config servers
        String snsTopicArn = System.getenv("CONFIG_TOPIC_ARN");
        if (snsTopicArn != null && !snsTopicArn.isEmpty()) {
            sendSnsNotification(snsTopicArn, serviceName, environment, configKey);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 200);
        response.put("message", "Config update notification processed");
        response.put("service", serviceName);
        response.put("environment", environment);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    private Map<String, Object> handleConfigValidation(Map<String, Object> event, Context context) {
        String serviceName = (String) event.get("serviceName");
        String configValue = (String) event.get("configValue");

        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate JSON structure
            objectMapper.readTree(configValue);
            
            context.getLogger().log("Config validation successful for: " + serviceName);
            
            ObjectNode validResponse = objectMapper.createObjectNode();
            validResponse.put("valid", true);
            validResponse.put("message", "Configuration is valid");
            
            response.put("statusCode", 200);
            response.put("body", validResponse);
            return response;
            
        } catch (JsonProcessingException e) {
            context.getLogger().log("Config validation failed: " + e.getMessage());
            
            ObjectNode errorResponse = objectMapper.createObjectNode();
            errorResponse.put("valid", false);
            errorResponse.put("message", "Configuration is invalid");
            errorResponse.put("error", e.getMessage());
            
            response.put("statusCode", 400);
            response.put("body", errorResponse);
            
            recordMetric(serviceName, "validation-error", "ConfigValidationErrors");
            return response;
        }
    }

    private void recordMetric(String serviceName, String dimension2, String metricName) {
        try {
            PutMetricDataRequest request = PutMetricDataRequest.builder()
                .namespace("SmartPayHub/ConfigService")
                .metricData(
                    MetricDatum.builder()
                        .metricName(metricName)
                        .value(1.0)
                        .unit(StandardUnit.COUNT)
                        .timestamp(Instant.now())
                        .dimensions(
                            Dimension.builder().name("ServiceName").value(serviceName).build(),
                            Dimension.builder().name("Type").value(dimension2).build()
                        )
                        .build()
                )
                .build();

            cloudWatchClient.putMetricData(request);
        } catch (CloudWatchException e) {
            // Log but don't throw - metrics should never block config update
            System.err.println("Failed to record metric: " + e.getMessage());
        }
    }

    private void sendSnsNotification(String topicArn, String serviceName, String environment, String configKey) {
        try {
            String message = String.format(
                "Config updated: %s/%s/%s",
                serviceName, environment, configKey
            );

            PublishRequest request = PublishRequest.builder()
                .topicArn(topicArn)
                .message(message)
                .subject("SmartPayHub Config Update")
                .build();

            PublishResponse result = snsClient.publish(request);
            System.out.println("SNS message published: " + result.messageId());
            
        } catch (SnsException e) {
            System.err.println("Failed to send SNS notification: " + e.getMessage());
        }
    }
}