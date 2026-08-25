package com.cloudops.manager.aws.observability.model;

import java.time.Instant;
import java.util.List;

public record MetricSeries(
    String metricName,
    String instanceId,
    String region,
    String accountId,
    String unit,
    String statistic,
    int periodSeconds,
    Instant startTime,
    Instant endTime,
    List<MetricDataPoint> dataPoints
) {}