package com.cloudops.manager.aws.discovery.service;

import com.cloudops.manager.aws.discovery.config.AwsClientFactory;
import com.cloudops.manager.aws.discovery.model.*;
import com.cloudops.manager.aws.discovery.provider.*;
import com.cloudops.manager.aws.sts.model.AssumeRoleRequest;
import com.cloudops.manager.aws.sts.model.AssumedRoleSession;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsResponse;
import software.amazon.awssdk.services.ec2.paginators.DescribeInstancesIterable;
import software.amazon.awssdk.services.ec2.paginators.DescribeSecurityGroupsIterable;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.paginators.DescribeDBInstancesIterable;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwsResourceDiscoveryServiceTest {

    @Mock
    private AwsIdentityService awsIdentityService;
    @Mock
    private Ec2Provider ec2Provider;
    @Mock
    private S3Provider s3Provider;
    @Mock
    private RdsProvider rdsProvider;
    @Mock
    private VpcProvider vpcProvider;
    @Mock
    private SecurityGroupProvider securityGroupProvider;
    @Mock
    private IamProvider iamProvider;
    @Mock
    private AwsClientFactory awsClientFactory;

    @InjectMocks
    private AwsResourceDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(discoveryService, "defaultRegion", "us-east-1");
    }

    @Test
    @DisplayName("Should aggregate all discovered resources in InventorySummary")
    void shouldDiscoverAllResources() {
        when(awsIdentityService.getCurrentIdentity())
                .thenReturn(new CallerIdentity("123456789012", "arn:aws:iam::123456789012:user/admin", "AIDADMIN"));

        Ec2InstanceResource ec2 = new Ec2InstanceResource(
                "i-1", CloudResourceType.EC2_INSTANCE, "web", "us-east-1", "123456789012", "running",
                "arn:ec2", Collections.emptyMap(), Instant.now(), "t3.micro", "10.0.0.1", null, null, null, null, null, null
        );
        S3BucketResource s3 = new S3BucketResource(
                "b-1", CloudResourceType.S3_BUCKET, "b-1", "us-east-1", "123456789012", "ACTIVE",
                "arn:s3", Collections.emptyMap(), Instant.now(), Instant.now()
        );

        when(ec2Provider.describeInstances("us-east-1", "123456789012")).thenReturn(List.of(ec2));
        when(s3Provider.listBuckets("us-east-1", "123456789012")).thenReturn(List.of(s3));
        when(rdsProvider.describeDbInstances("us-east-1", "123456789012")).thenReturn(Collections.emptyList());
        when(vpcProvider.describeVpcs("us-east-1", "123456789012")).thenReturn(Collections.emptyList());
        when(securityGroupProvider.describeSecurityGroups("us-east-1", "123456789012")).thenReturn(Collections.emptyList());

        InventorySummary summary = discoveryService.discoverAll(null);

        assertThat(summary.accountId()).isEqualTo("123456789012");
        assertThat(summary.region()).isEqualTo("us-east-1");
        assertThat(summary.totalCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should perform cross-account discovery using scoped clients")
    void shouldDiscoverCrossAccount() {
        AwsAccountTarget target = new AwsAccountTarget(
                "987654321098", "arn:aws:iam::987654321098:role/CrossAccountRole", "session-1", null, "us-east-1"
        );
        AssumedRoleSession session = new AssumedRoleSession("ASIAKEY", "SECRET", "TOKEN", Instant.now().plusSeconds(900), target.roleArn());
        when(awsIdentityService.assumeRole(any(AssumeRoleRequest.class))).thenReturn(session);

        StsClient mockSts = mock(StsClient.class);
        Ec2Client mockEc2 = mock(Ec2Client.class);
        S3Client mockS3 = mock(S3Client.class);
        RdsClient mockRds = mock(RdsClient.class);

        when(awsClientFactory.createStsClient(any(), any())).thenReturn(mockSts);
        when(awsClientFactory.createEc2Client(any(), any())).thenReturn(mockEc2);
        when(awsClientFactory.createS3Client(any(), any())).thenReturn(mockS3);
        when(awsClientFactory.createRdsClient(any(), any())).thenReturn(mockRds);

        when(mockSts.getCallerIdentity(any(GetCallerIdentityRequest.class)))
                .thenReturn(GetCallerIdentityResponse.builder().account("987654321098").build());

        DescribeInstancesIterable ec2Paginator = mock(DescribeInstancesIterable.class);
        when(ec2Paginator.iterator()).thenReturn(List.<DescribeInstancesResponse>of().iterator());
        when(mockEc2.describeInstancesPaginator(any(DescribeInstancesRequest.class))).thenReturn(ec2Paginator);

        when(mockS3.listBuckets()).thenReturn(ListBucketsResponse.builder().buckets(List.of()).build());

        DescribeDBInstancesIterable rdsPaginator = mock(DescribeDBInstancesIterable.class);
        when(rdsPaginator.iterator()).thenReturn(List.<DescribeDbInstancesResponse>of().iterator());
        when(mockRds.describeDBInstancesPaginator(any(DescribeDbInstancesRequest.class))).thenReturn(rdsPaginator);

        when(mockEc2.describeVpcs(any(DescribeVpcsRequest.class))).thenReturn(DescribeVpcsResponse.builder().build());

        DescribeSecurityGroupsIterable sgPaginator = mock(DescribeSecurityGroupsIterable.class);
        when(sgPaginator.iterator()).thenReturn(List.<DescribeSecurityGroupsResponse>of().iterator());
        when(mockEc2.describeSecurityGroupsPaginator(any(DescribeSecurityGroupsRequest.class))).thenReturn(sgPaginator);

        InventorySummary summary = discoveryService.discoverAccount(target);

        assertThat(summary.accountId()).isEqualTo("987654321098");
        assertThat(summary.region()).isEqualTo("us-east-1");
        assertThat(summary.totalCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should reject cross-account discovery if verified account does not match target")
    void shouldRejectAccountMismatch() {
        AwsAccountTarget target = new AwsAccountTarget(
                "987654321098", "arn:aws:iam::987654321098:role/CrossAccountRole", "session-1", null, "us-east-1"
        );
        AssumedRoleSession session = new AssumedRoleSession("ASIAKEY", "SECRET", "TOKEN", Instant.now().plusSeconds(900), target.roleArn());
        when(awsIdentityService.assumeRole(any(AssumeRoleRequest.class))).thenReturn(session);

        StsClient mockSts = mock(StsClient.class);
        Ec2Client mockEc2 = mock(Ec2Client.class);
        S3Client mockS3 = mock(S3Client.class);
        RdsClient mockRds = mock(RdsClient.class);

        when(awsClientFactory.createStsClient(any(), any())).thenReturn(mockSts);
        when(awsClientFactory.createEc2Client(any(), any())).thenReturn(mockEc2);
        when(awsClientFactory.createS3Client(any(), any())).thenReturn(mockS3);
        when(awsClientFactory.createRdsClient(any(), any())).thenReturn(mockRds);

        when(mockSts.getCallerIdentity(any(GetCallerIdentityRequest.class)))
                .thenReturn(GetCallerIdentityResponse.builder().account("000000000000").build());

        assertThatThrownBy(() -> discoveryService.discoverAccount(target))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not match requested target account");
    }

    @Test
    @DisplayName("Should retrieve IAM user detail successfully")
    void shouldGetIamUserDetail() {
        when(awsIdentityService.getCurrentIdentity())
                .thenReturn(new CallerIdentity("123456789012", "arn:aws:iam::123456789012:user/admin", "AIDADMIN"));

        IamUserDetailResource mockUser = new IamUserDetailResource(
                "john", "AIDAJOHN", "arn:aws:iam::123456789012:user/john", "/", Instant.now(), "123456789012",
                true, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyMap(), Instant.now()
        );

        when(iamProvider.getUser("john", "123456789012")).thenReturn(Optional.of(mockUser));

        IamUserDetailResource result = discoveryService.getIamUserDetail("john");

        assertThat(result).isNotNull();
        assertThat(result.userName()).isEqualTo("john");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when IAM user not found")
    void shouldThrowWhenUserNotFound() {
        when(awsIdentityService.getCurrentIdentity())
                .thenReturn(new CallerIdentity("123456789012", "arn:aws:iam::123456789012:user/admin", "AIDADMIN"));

        when(iamProvider.getUser("missing", "123456789012")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> discoveryService.getIamUserDetail("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing");
    }
}