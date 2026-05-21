package com.cloudconfig.smart.pay.hub.sph.service.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import io.micrometer.core.annotation.Timed;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified Parameter Store Service using in-memory cache
 * In production, integrate with AWS Systems Manager Parameter Store
 */
@Slf4j
@Service
public class AwsParameterStoreService {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    // In-memory cache for parameters (replace with Redis in production)
    private final Map<String, String> parameterCache = new ConcurrentHashMap<>();

    /**
     * Store config in Parameter Store
     * Path: /smartpayhub/{serviceName}/{environment}/{configKey}
     */
    @Timed(value = "config.parameter-store.put", description = "Parameter Store put time")
    public void putParameter(String serviceName, String environment, String configKey, String configValue) {
        try {
            String parameterName = String.format(
                    "/smartpayhub/%s/%s/%s",
                    serviceName, environment, configKey
            );

            // Store in cache
            parameterCache.put(parameterName, configValue);

            log.info("Parameter stored in cache: {} (Region: {})", parameterName, awsRegion);

        } catch (Exception e) {
            log.error("Failed to put parameter", e);
            throw new RuntimeException("Parameter Store put failed", e);
        }
    }

    @Timed(value = "config.parameter-store.get", description = "Parameter Store get time")
    public String getParameter(String serviceName, String environment, String configKey) {
        try {
            String parameterName = String.format(
                    "/smartpayhub/%s/%s/%s",
                    serviceName, environment, configKey
            );

            String value = parameterCache.get(parameterName);

            if (value == null) {
                log.warn("Parameter not found: {}/{}/{}", serviceName, environment, configKey);
                return null;
            }

            log.info("Parameter retrieved: {}", parameterName);
            return value;

        } catch (Exception e) {
            log.error("Failed to get parameter", e);
            throw new RuntimeException("Parameter Store get failed", e);
        }
    }

    /**
     * Get all parameters for a service/environment
     */
    @Timed(value = "config.parameter-store.list", description = "Parameter Store list time")
    public List<String> getAllParameters(String serviceName, String environment) {
        try {
            String pathPrefix = String.format("/smartpayhub/%s/%s/", serviceName, environment);

            List<String> parameters = parameterCache.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(pathPrefix))
                    .map(Map.Entry::getValue)
                    .toList();

            log.info("Retrieved {} parameters for {}/{}", parameters.size(), serviceName, environment);
            return parameters;

        } catch (Exception e) {
            log.error("Failed to get parameters by path", e);
            throw new RuntimeException("Parameter Store path query failed", e);
        }
    }

    /**
     * Delete parameter from Parameter Store
     */
    public void deleteParameter(String serviceName, String environment, String configKey) {
        try {
            String parameterName = String.format(
                    "/smartpayhub/%s/%s/%s",
                    serviceName, environment, configKey
            );

            parameterCache.remove(parameterName);
            log.info("Parameter deleted: {}", parameterName);

        } catch (Exception e) {
            log.error("Failed to delete parameter", e);
            throw new RuntimeException("Parameter Store delete failed", e);
        }
    }

    /**
     * Get cache size (for monitoring)
     */
    public int getCacheSize() {
        return parameterCache.size();
    }

    /**
     * Clear all cached parameters
     */
    public void clearCache() {
        parameterCache.clear();
        log.info("Parameter cache cleared");
    }
}