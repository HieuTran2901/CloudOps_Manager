package com.cloudops.manager.aws.observability.provider;

import com.cloudops.manager.aws.observability.model.MetricDataQueryRequest;
import com.cloudops.manager.aws.observability.model.MetricSeries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwsCloudWatchMetricsProviderTest {

    @Mock
    private CloudWatchClient cloudWatchClient;

    private AwsCloudWatchMetricsProvider provider;

    @BeforeEach
    void setUp() {
        provider = new AwsCloudWatchMetricsProvider(cloudWatchClient);
    }

    @Test
    @DisplayName("Should execute GetMetricData with NextToken pagination")
    void shouldExecuteGetMetricDataWithPagination() {
        Instant now = Instant.now();
        MetricDataQueryRequest req = new MetricDataQueryRequest(
                "AWS/EC2", "CPUUtilization", Map.of("InstanceId", "i-123"), "Average", 300,
                now.minusSeconds(3600), now, "i-123", "EC2", "123456789012", "us-east-1", null, null
        );

        MetricDataResult page1 = MetricDataResult.builder().id("q_0").timestamps(now.minusSeconds(1800)).values(45.5).build();
        MetricDataResult page2 = MetricDataResult.builder().id("q_0").timestamps(now.minusSeconds(900)).values(55.2).build();

        when(cloudWatchClient.getMetricData(any(GetMetricDataRequest.class)))
                .thenReturn(GetMetricDataResponse.builder().metricDataResults(page1).nextToken("tok-1").build())
                .thenReturn(GetMetricDataResponse.builder().metricDataResults(page2).nextToken(null).build());

        List<MetricSeries> series = provider.queryMetricData(List.of(req), cloudWatchClient);

        assertThat(series).hasSize(1);
        assertThat(series.get(0).metricName()).isEqualTo("CPUUtilization");
        assertThat(series.get(0).dataPoints()).hasSize(2);
    }
}