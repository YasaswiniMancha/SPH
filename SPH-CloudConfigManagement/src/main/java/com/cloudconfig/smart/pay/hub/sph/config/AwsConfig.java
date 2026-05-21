package com.cloudconfig.smart.pay.hub.sph.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class AwsConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public S3Client s3Client() {
        log.info("Initializing S3Client for region: {}", awsRegion);
        return S3Client.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .region(Region.of(awsRegion))
            .build();
    }

    @Bean
    public CloudWatchClient cloudWatchClient() {
        log.info("Initializing CloudWatchClient for region: {}", awsRegion);
        return CloudWatchClient.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .region(Region.of(awsRegion))
            .build();
    }

    @Bean
    public LambdaClient lambdaClient() {
        log.info("Initializing LambdaClient for region: {}", awsRegion);
        return LambdaClient.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .region(Region.of(awsRegion))
            .build();
    }

    @Bean
    public Ec2Client ec2Client() {
        log.info("Initializing EC2Client for region: {}", awsRegion);
        return Ec2Client.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .region(Region.of(awsRegion))
            .build();
    }

    @Bean
    public SnsClient snsClient() {
        log.info("Initializing SnsClient for region: {}", awsRegion);
        return SnsClient.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .region(Region.of(awsRegion))
            .build();
    }
}