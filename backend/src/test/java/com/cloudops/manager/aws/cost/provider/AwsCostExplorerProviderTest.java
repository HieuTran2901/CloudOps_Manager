package com.cloudops.manager.aws.cost.provider;

import com.cloudops.manager.aws.cost.model.CostAggregationResult;
import com.cloudops.manager.aws.cost.model.CostQueryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.costexplorer.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwsCostExplorerProviderTest {

    @Mock
    private CostExplorerClient costExplorerClient;

    private AwsCostExplorerProvider provider;

    @BeforeEach
    void setUp() {
        provider = new AwsCostExplorerProvider(costExplorerClient);
    }

    @Test
    @DisplayName("Should parse GetCostAndUsage response with exact BigDecimal precision and groups")
    void shouldParseCostAndUsage() {
        CostQueryRequest request = new CostQueryRequest(
                "123456789012", "UnblendedCost", "MONTHLY",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                List.of("SERVICE"), null
        );

        Group ec2Group = Group.builder()
                .keys("Amazon Elastic Compute Cloud - Compute")
                .metrics(Map.of("UnblendedCost", MetricValue.builder().amount("125.42").unit("USD").build()))
                .build();

        ResultByTime rbt = ResultByTime.builder()
                .timePeriod(DateInterval.builder().start("2026-01-01").end("2026-02-01").build())
                .total(Map.of("UnblendedCost", MetricValue.builder().amount("125.42").unit("USD").build()))
                .groups(ec2Group)
                .build();

        GetCostAndUsageResponse response = GetCostAndUsageResponse.builder()
                .resultsByTime(rbt)
                .build();

        when(costExplorerClient.getCostAndUsage(any(GetCostAndUsageRequest.class))).thenReturn(response);

        CostAggregationResult result = provider.getCostAndUsage(request, costExplorerClient, "STANDALONE_ACCOUNT");

        assertThat(result.accountId()).isEqualTo("123456789012");
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("125.42"));
        assertThat(result.unit()).isEqualTo("USD");
        assertThat(result.resultsByTime()).hasSize(1);
        assertThat(result.resultsByTime().get(0).groups()).hasSize(1);
        assertThat(result.resultsByTime().get(0).groups().get(0).amount()).isEqualByComparingTo(new BigDecimal("125.42"));
    }
}