package com.cloudconfig.smart.pay.hub.sph.service.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;
import io.micrometer.core.annotation.Timed;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class AwsS3ConfigService {

    private final S3Client s3Client;
    
    @Value("${aws.s3.bucket}")
    private String bucketName;
    
    @Value("${aws.s3.prefix}")
    private String prefix;

    public AwsS3ConfigService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Timed(value = "config.s3.upload", description = "S3 config upload time")
    public String uploadConfig(String serviceName, String environment, 
                               String configKey, String configValue, Long version) {
        try {
            String key = String.format("%s%s/%s/%s/v%d.json", 
                prefix, serviceName, environment, configKey, version);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/json")
                .metadata(java.util.Map.of(
                    "service", serviceName,
                    "environment", environment,
                    "configKey", configKey
                ))
                .build();

            s3Client.putObject(putObjectRequest, 
                RequestBody.fromString(configValue, StandardCharsets.UTF_8));

            log.info("Config uploaded to S3: s3://{}/{}", bucketName, key);
            return key;
        } catch (S3Exception e) {
            log.error("Failed to upload config to S3", e);
            throw new RuntimeException("S3 upload failed", e);
        }
    }

    @Timed(value = "config.s3.download", description = "S3 config download time")
    public String downloadConfig(String s3Key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

            return s3Client.getObjectAsBytes(request).asUtf8String();
        } catch (S3Exception e) {
            log.error("Failed to download config from S3: {}", s3Key, e);
            throw new RuntimeException("S3 download failed", e);
        }
    }

    @Timed(value = "config.s3.list", description = "S3 config list time")
    public List<String> listConfigs(String serviceName, String environment) {
        try {
            String searchPrefix = String.format("%s%s/%s/", prefix, serviceName, environment);
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(searchPrefix)
                .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            return response.contents().stream()
                .map(S3Object::key)
                .toList();
        } catch (S3Exception e) {
            log.error("Failed to list configs from S3", e);
            throw new RuntimeException("S3 list failed", e);
        }
    }

    @Timed(value = "config.s3.delete", description = "S3 config delete time")
    public void deleteConfig(String s3Key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
            
            s3Client.deleteObject(request);
            log.info("Config deleted from S3: {}", s3Key);
        } catch (S3Exception e) {
            log.error("Failed to delete config from S3: {}", s3Key, e);
            throw new RuntimeException("S3 delete failed", e);
        }
    }
}