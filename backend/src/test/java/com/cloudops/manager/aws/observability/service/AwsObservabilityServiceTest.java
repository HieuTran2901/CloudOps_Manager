package com.cloudops.manager.aws.observability.service;

import com.cloudops.manager.aws.discovery.config.AwsClientFactory;
import com.cloudops.manager.aws.observability.model.MetricDataPoint;
import com.cloudops.manager.aws.observability.model.MetricSeries;
import com.cloudops.manager.aws.observability.model.TelemetryAggregationResult;
import com.cloudops.manager.aws.observability.provider.CloudWatchMetricsProvider;
import com.cloudops.manager.aws.sts.model.AssumeRoleRequest;
import com.cloudops.manager.aws.sts.model.AssumedRoleSession;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
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
class AwsObservabilityServiceTest {

    @Mock
    private CloudWatchMetricsProvider metricsProvider;
    @Mock
    private AwsIdentityService awsIdentityService;
    @Mock
    private AwsClientFactory awsClientFactory;

    @InjectMocks
    private AwsObservabilityService observabilityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(observabilityService, "defaultRegion", "us-east-1");
    }

    @Test
    @DisplayName("Should query aggregated metrics locally")
    void shouldQueryAggregatedMetricsLocally() {
        when(awsIdentityService.getCurrentIdentity())
                .thenReturn(new CallerIdentity("123456789012", "arn:aws:iam::123456789012:user/admin", "AIDADMIN"));

        MetricSeries s = new MetricSeries("CPUUtilization", "i-123", "us-east-1", "123456789012", "Percent", "Average", 300, Instant.now().minusSeconds(3600), Instant.now(), List.of(new MetricDataPoint(Instant.now(), 50.0, "Percent", "Average")));
        when(metricsProvider.queryMetricData(any(), any())).thenReturn(List.of(s));

        TelemetryAggregationResult result = observabilityService.getAggregatedMetrics(
                "EC2", List.of("i-123"), List.of("CPUUtilization"),
                Instant.now().minusSeconds(3600), Instant.now(), 300, "Average", null, null, "us-east-1"
        );

        assertThat(result.accountId()).isEqualTo("123456789012");
        assertThat(result.totalMetrics()).isEqualTo(1);
        assertThat(result.totalDatapoints()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should query cross-account telemetry via STS AssumeRole")
    void shouldQueryCrossAccountTelemetry() {
        AwsAccountTarget target = new AwsAccountTarget("987654321098", "arn:aws:iam::987654321098:role/CrossAccountTelemetryRole", null, null, "us-east-1");
        AssumedRoleSession session = new AssumedRoleSession("ASIAKEY", "SECRET", "TOKEN", Instant.now().plusSeconds(900), target.roleArn());
        when(awsIdentityService.assumeRole(any(AssumeRoleRequest.class))).thenReturn(session);

        StsClient mockSts = mock(StsClient.class);
        CloudWatchClient mockCw = mock(CloudWatchClient.class);
        when(awsClientFactory.createStsClient(any(), any())).thenReturn(mockSts);
        when(awsClientFactory.createCloudWatchClient(any(), any())).thenReturn(mockCw);

        when(mockSts.getCallerIdentity(any(GetCallerIdentityRequest.class)))
                .thenReturn(GetCallerIdentityResponse.builder().account("987654321098").build());

        MetricSeries s = new MetricSeries("CPUUtilization", "i-123", "us-east-1", "987654321098", "Percent", "Average", 300, Instant.now().minusSeconds(3600), Instant.now(), List.of());
        when(metricsProvider.queryMetricData(any(), any())).thenReturn(List.of(s));

        TelemetryAggregationResult result = observabilityService.getCrossAccountMetrics(
                target, "EC2", List.of("i-123"), List.of("CPUUtilization"),
                Instant.now().minusSeconds(3600), Instant.now(), 300, "Average", null, null
        );

        assertThat(result.accountId()).isEqualTo("987654321098");
        assertThat(result.totalMetrics()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reject invalid period not multiple of 60")
    void shouldRejectInvalidPeriod() {
        when(awsIdentityService.getCurrentIdentity())
                .thenReturn(new CallerIdentity("123456789012", "arn:aws:iam::123456789012:user/admin", "AIDADMIN"));

        assertThatThrownBy(() -> observabilityService.getAggregatedMetrics(
                "EC2", List.of("i-123"), List.of("CPUUtilization"),
                Instant.now().minusSeconds(3600), Instant.now(), 45, "Average", null, null, "us-east-1"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("multiple of 60");
    }
}