package com.cloudops.manager.aws.sts.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;

@Configuration
public class AwsStsConfig {

    private static final Logger log = LoggerFactory.getLogger(AwsStsConfig.class);

    @Value("${cloudops.aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public StsClient stsClient() {
        log.info("Initializing AWS STS Client with region: {}", awsRegion);
        return StsClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }
}