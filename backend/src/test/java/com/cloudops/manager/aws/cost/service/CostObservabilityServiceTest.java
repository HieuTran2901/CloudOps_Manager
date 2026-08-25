package com.cloudops.manager.aws.cost.service;

import com.cloudops.manager.aws.cost.model.CostAggregationResult;
import com.cloudops.manager.aws.cost.model.CostTimePeriod;
import com.cloudops.manager.aws.cost.provider.CostExplorerProvider;
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
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CostObservabilityServiceTest {

    @Mock
    private CostExplorerProvider costExplorerProvider;
    @Mock
    private AwsIdentityService awsIdentityService;
    @Mock
    private AwsClientFactory awsClientFactory;

    @InjectMocks
    private CostObservabilityService costObservabilityService;

    @Test
    @DisplayName("Should query local Cost Explorer data")
    void shouldQueryLocalCost() {
        when(awsIdentityService.getCurrentIdentity())
                .thenReturn(new CallerIdentity("123456789012", "arn:aws:iam::123456789012:user/admin", "AIDADMIN"));

        CostAggregationResult mockResult = new CostAggregationResult(
                "123456789012", "STANDALONE_ACCOUNT", "UnblendedCost", "MONTHLY",
                new CostTimePeriod("2026-01-01", "2026-02-01"),
                new BigDecimal("500.00"), "USD", List.of(), Instant.now()
        );

        when(costExplorerProvider.getCostAndUsage(any(), any(), any())).thenReturn(mockResult);

        CostAggregationResult result = costObservabilityService.getCostAndUsage(
                "UnblendedCost", "MONTHLY", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), List.of("SERVICE"), null
        );

        assertThat(result.accountId()).isEqualTo("123456789012");
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("Should query cross-account Cost Explorer data via STS AssumeRole")
    void shouldQueryCrossAccountCost() {
        AwsAccountTarget target = new AwsAccountTarget("987654321098", "arn:aws:iam::987654321098:role/CostAuditRole", null, null, "us-east-1");
        AssumedRoleSession session = new AssumedRoleSession("ASIAKEY", "SECRET", "TOKEN", Instant.now().plusSeconds(900), target.roleArn());
        when(awsIdentityService.assumeRole(any(AssumeRoleRequest.class))).thenReturn(session);

        StsClient mockSts = mock(StsClient.class);
        CostExplorerClient mockCe = mock(CostExplorerClient.class);
        when(awsClientFactory.createStsClient(any(), any())).thenReturn(mockSts);
        when(awsClientFactory.createCostExplorerClient(any())).thenReturn(mockCe);

        when(mockSts.getCallerIdentity(any(GetCallerIdentityRequest.class)))
                .thenReturn(GetCallerIdentityResponse.builder().account("987654321098").build());

        CostAggregationResult mockResult = new CostAggregationResult(
                "987654321098", "CROSS_ACCOUNT_ASSUMED", "UnblendedCost", "MONTHLY",
                new CostTimePeriod("2026-01-01", "2026-02-01"),
                new BigDecimal("300.00"), "USD", List.of(), Instant.now()
        );
        when(costExplorerProvider.getCostAndUsage(any(), any(), any())).thenReturn(mockResult);

        CostAggregationResult result = costObservabilityService.getCrossAccountCostAndUsage(
                target, "UnblendedCost", "MONTHLY", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), null, null
        );

        assertThat(result.accountId()).isEqualTo("987654321098");
        assertThat(result.billingScope()).isEqualTo("CROSS_ACCOUNT_ASSUMED");
    }

    @Test
    @DisplayName("Should reject cross-account query if caller identity account mismatches target")
    void shouldRejectAccountMismatch() {
        AwsAccountTarget target = new AwsAccountTarget("987654321098", "arn:aws:iam::987654321098:role/CostAuditRole", null, null, "us-east-1");
        AssumedRoleSession session = new AssumedRoleSession("ASIAKEY", "SECRET", "TOKEN", Instant.now().plusSeconds(900), target.roleArn());
        when(awsIdentityService.assumeRole(any(AssumeRoleRequest.class))).thenReturn(session);

        StsClient mockSts = mock(StsClient.class);
        CostExplorerClient mockCe = mock(CostExplorerClient.class);
        when(awsClientFactory.createStsClient(any(), any())).thenReturn(mockSts);
        when(awsClientFactory.createCostExplorerClient(any())).thenReturn(mockCe);

        when(mockSts.getCallerIdentity(any(GetCallerIdentityRequest.class)))
                .thenReturn(GetCallerIdentityResponse.builder().account("000000000000").build());

        assertThatThrownBy(() -> costObservabilityService.getCrossAccountCostAndUsage(
                target, "UnblendedCost", "MONTHLY", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), null, null
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not match target account");
    }
}