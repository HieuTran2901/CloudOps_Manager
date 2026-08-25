package com.cloudops.manager.aws.observability.provider;

import com.cloudops.manager.aws.observability.model.MetricDataQueryRequest;
import com.cloudops.manager.aws.observability.model.MetricQuery;
import com.cloudops.manager.aws.observability.model.MetricSeries;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

import java.util.List;

public interface CloudWatchMetricsProvider {
    MetricSeries getEc2Metric(MetricQuery query);
    MetricSeries getRdsMetric(MetricQuery query);
    List<MetricSeries> queryMetricData(List<MetricDataQueryRequest> requests, CloudWatchClient client);
}