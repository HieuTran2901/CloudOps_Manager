package com.cloudops.manager.aws.discovery.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.servicequotas.ServiceQuotasClient;

@Configuration
public class AwsClientConfig {

    private static final Logger log = LoggerFactory.getLogger(AwsClientConfig.class);

    @Value("${cloudops.aws.region:us-east-1}")
    private String defaultRegion;

    @Bean
    public IamClient iamClient() {
        log.info("Configuring AWS IAM Client for global region");
        return IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public Ec2Client ec2Client() {
        log.info("Configuring AWS EC2 Client for region: {}", defaultRegion);
        return Ec2Client.builder()
                .region(Region.of(defaultRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public S3Client s3Client() {
        log.info("Configuring AWS S3 Client for region: {}", defaultRegion);
        return S3Client.builder()
                .region(Region.of(defaultRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public RdsClient rdsClient() {
        log.info("Configuring AWS RDS Client for region: {}", defaultRegion);
        return RdsClient.builder()
                .region(Region.of(defaultRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public CloudWatchClient cloudWatchClient() {
        log.info("Configuring AWS CloudWatch Client for region: {}", defaultRegion);
        return CloudWatchClient.builder()
                .region(Region.of(defaultRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public CostExplorerClient costExplorerClient() {
        log.info("Configuring AWS Cost Explorer Client for us-east-1");
        return CostExplorerClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public CloudTrailClient cloudTrailClient() {
        log.info("Configuring AWS CloudTrail Client for region: {}", defaultRegion);
        return CloudTrailClient.builder()
                .region(Region.of(defaultRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public ServiceQuotasClient serviceQuotasClient() {
        log.info("Configuring AWS Service Quotas Client for region: {}", defaultRegion);
        return ServiceQuotasClient.builder()
                .region(Region.of(defaultRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}