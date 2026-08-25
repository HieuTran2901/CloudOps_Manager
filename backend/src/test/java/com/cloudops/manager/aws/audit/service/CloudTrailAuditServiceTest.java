package com.cloudops.manager.aws.audit.service;

import com.cloudops.manager.aws.audit.model.CloudTrailEventResult;
import com.cloudops.manager.aws.audit.provider.CloudTrailProvider;
import com.cloudops.manager.aws.discovery.config.AwsClientFactory;
import com.cloudops.manager.aws.sts.model.AssumeRoleRequest;
import com.cloudops.manager.aws.sts.model.AssumedRoleSession;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudTrailAuditServiceTest {

    @Mock
    private CloudTrailProvider cloudTrailProvider;
    @Mock
    private AwsIdentityService awsIdentityService;
    @Mock
    private AwsClientFactory awsClientFactory;

    @InjectMocks
    private CloudTrailAuditService cloudTrailAuditService;

    @Test
    @DisplayName("Should query local CloudTrail events")
    void shouldQueryLocalCloudTrailEvents() {
        when(awsIdentityService.getCurrentIdentity())
                .thenReturn(new CallerIdentity("123456789012", "arn:aws:iam::123456789012:user/admin", "AIDADMIN"));

        CloudTrailEventResult mockResult = new CloudTrailEventResult(
                "123456789012", "us-east-1", Instant.now().minusSeconds(3600), Instant.now(), 0, List.of(), Instant.now()
        );
        when(cloudTrailProvider.lookupEvents(any(), any())).thenReturn(mockResult);

        CloudTrailEventResult result = cloudTrailAuditService.lookupEvents(
                "RunInstances", null, null, null, Instant.now().minusSeconds(3600), Instant.now(), 50, "us-east-1"
        );

        assertThat(result.accountId()).isEqualTo("123456789012");
        assertThat(result.totalEvents()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should query cross-account CloudTrail events via STS AssumeRole")
    void shouldQueryCrossAccountCloudTrailEvents() {
        AwsAccountTarget target = new AwsAccountTarget("987654321098", "arn:aws:iam::987654321098:role/AuditRole", null, null, "us-east-1");
        AssumedRoleSession session = new AssumedRoleSession("ASIAKEY", "SECRET", "TOKEN", Instant.now().plusSeconds(900), target.roleArn());
        when(awsIdentityService.assumeRole(any(AssumeRoleRequest.class))).thenReturn(session);

        StsClient mockSts = mock(StsClient.class);
        CloudTrailClient mockCt = mock(CloudTrailClient.class);
        when(awsClientFactory.createStsClient(any(), any())).thenReturn(mockSts);
        when(awsClientFactory.createCloudTrailClient(any(), any())).thenReturn(mockCt);

        when(mockSts.getCallerIdentity(any(GetCallerIdentityRequest.class)))
                .thenReturn(GetCallerIdentityResponse.builder().account("987654321098").build());

        CloudTrailEventResult mockResult = new CloudTrailEventResult(
                "987654321098", "us-east-1", Instant.now().minusSeconds(3600), Instant.now(), 0, List.of(), Instant.now()
        );
        when(cloudTrailProvider.lookupEvents(any(), any())).thenReturn(mockResult);

        CloudTrailEventResult result = cloudTrailAuditService.lookupCrossAccountEvents(
                target, "RunInstances", null, null, null, Instant.now().minusSeconds(3600), Instant.now(), 50
        );

        assertThat(result.accountId()).isEqualTo("987654321098");
    }

    @Test
    @DisplayName("Should reject cross-account query on account mismatch")
    void shouldRejectAccountMismatch() {
        AwsAccountTarget target = new AwsAccountTarget("987654321098", "arn:aws:iam::987654321098:role/AuditRole", null, null, "us-east-1");
        AssumedRoleSession session = new AssumedRoleSession("ASIAKEY", "SECRET", "TOKEN", Instant.now().plusSeconds(900), target.roleArn());
        when(awsIdentityService.assumeRole(any(AssumeRoleRequest.class))).thenReturn(session);

        StsClient mockSts = mock(StsClient.class);
        CloudTrailClient mockCt = mock(CloudTrailClient.class);
        when(awsClientFactory.createStsClient(any(), any())).thenReturn(mockSts);
        when(awsClientFactory.createCloudTrailClient(any(), any())).thenReturn(mockCt);

        when(mockSts.getCallerIdentity(any(GetCallerIdentityRequest.class)))
                .thenReturn(GetCallerIdentityResponse.builder().account("000000000000").build());

        assertThatThrownBy(() -> cloudTrailAuditService.lookupCrossAccountEvents(
                target, "RunInstances", null, null, null, Instant.now().minusSeconds(3600), Instant.now(), 50
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not match target account");
    }
}