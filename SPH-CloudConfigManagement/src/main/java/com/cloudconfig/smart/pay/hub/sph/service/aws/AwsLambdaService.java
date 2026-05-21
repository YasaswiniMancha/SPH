package com.cloudconfig.smart.pay.hub.sph.service.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;

@Slf4j
@Service
public class AwsLambdaService {

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.lambda.function.name}")
    private String lambdaFunctionName;

    public AwsLambdaService(LambdaClient lambdaClient, ObjectMapper objectMapper) {
        this.lambdaClient = lambdaClient;
        this.objectMapper = objectMapper;
    }

    @Timed(value = "config.lambda.invoke", description = "Lambda invocation time")
    public String notifyConfigUpdate(String serviceName, String environment, String configKey) {
        try {
            var payload = java.util.Map.of(
                "action", "CONFIG_UPDATED",
                "serviceName", serviceName,
                "environment", environment,
                "configKey", configKey,
                "timestamp", System.currentTimeMillis()
            );

            InvokeRequest request = InvokeRequest.builder()
                .functionName(lambdaFunctionName)
                .invocationType(InvocationType.EVENT) // Async invocation
                .payload(SdkBytes.fromString(objectMapper.writeValueAsString(payload), 
                    java.nio.charset.StandardCharsets.UTF_8))
                .build();

            InvokeResponse response = lambdaClient.invoke(request);
            
            log.info("Lambda function invoked. Function: {}, Status: {}", 
                lambdaFunctionName, response.statusCode());
            
            return response.logResult() != null ? response.logResult() : "";
        } catch (Exception e) {
            log.warn("Failed to invoke Lambda function", e);
            // Don't throw - allow config update to proceed even if notification fails
            return "";
        }
    }

    public boolean validateConfigWithLambda(String serviceName, String configValue) {
        try {
            var payload = java.util.Map.of(
                "action", "VALIDATE_CONFIG",
                "serviceName", serviceName,
                "configValue", configValue
            );

            InvokeRequest request = InvokeRequest.builder()
                .functionName(lambdaFunctionName)
                .invocationType(InvocationType.REQUEST_RESPONSE) // Sync call
                .payload(SdkBytes.fromString(objectMapper.writeValueAsString(payload),
                    java.nio.charset.StandardCharsets.UTF_8))
                .build();

            InvokeResponse response = lambdaClient.invoke(request);
            String responsePayload = response.payload().asUtf8String();
            
            var result = objectMapper.readTree(responsePayload);
            boolean isValid = result.has("valid") ? result.get("valid").asBoolean() : true;
            
            log.debug("Config validation result: {} for service: {}", isValid, serviceName);
            return isValid;
        } catch (Exception e) {
            log.warn("Config validation via Lambda failed, allowing config update", e);
            return true; // Don't block on validation failure
        }
    }
}