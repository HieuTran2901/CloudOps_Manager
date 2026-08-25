package com.cloudops.manager.aws.discovery.config;

import com.cloudops.manager.aws.sts.model.AssumedRoleSession;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;

@Component
public class AwsClientFactory {

    public Ec2Client createEc2Client(AssumedRoleSession session, String region) {
        return Ec2Client.builder()
                .region(Region.of(region))
                .credentialsProvider(createCredentialsProvider(session))
                .build();
    }

    public S3Client createS3Client(AssumedRoleSession session, String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(createCredentialsProvider(session))
                .build();
    }

    public RdsClient createRdsClient(AssumedRoleSession session, String region) {
        return RdsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(createCredentialsProvider(session))
                .build();
    }

    public IamClient createIamClient(AssumedRoleSession session) {
        return IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .credentialsProvider(createCredentialsProvider(session))
                .build();
    }

    public StsClient createStsClient(AssumedRoleSession session, String region) {
        return StsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(createCredentialsProvider(session))
                .build();
    }

    public CloudWatchClient createCloudWatchClient(AssumedRoleSession session, String region) {
        return CloudWatchClient.builder()
                .region(Region.of(region))
                .credentialsProvider(createCredentialsProvider(session))
                .build();
    }

    public CostExplorerClient createCostExplorerClient(AssumedRoleSession session) {
        return CostExplorerClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(createCredentialsProvider(session))
                .build();
    }

    public CloudTrailClient createCloudTrailClient(AssumedRoleSession session, String region) {
        return CloudTrailClient.builder()
                .region(Region.of(region))
                .credentialsProvider(createCredentialsProvider(session))
                .build();
    }

    private StaticCredentialsProvider createCredentialsProvider(AssumedRoleSession session) {
        AwsSessionCredentials credentials = AwsSessionCredentials.create(
                session.accessKeyId(),
                session.secretAccessKey(),
                session.sessionToken()
        );
        return StaticCredentialsProvider.create(credentials);
    }
}