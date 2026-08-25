package com.cloudops.manager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;

@SpringBootTest
class CloudOpsApplicationTests {

    @MockBean
    private StsClient stsClient;

    @MockBean
    private Ec2Client ec2Client;

    @MockBean
    private S3Client s3Client;

    @MockBean
    private RdsClient rdsClient;

    @MockBean
    private CloudWatchClient cloudWatchClient;

    @Test
    @DisplayName("Application context should load successfully without live AWS connection")
    void contextLoads() {
    }
}