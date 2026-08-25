package com.cloudops.manager.aws.observability.model;

import java.time.Instant;
import java.util.Map;

public record MetricDataQueryRequest(
    String namespace,
    String metricName,
    Map<String, String> dimensions,
    String statistic,
    int periodSeconds,
    Instant startTime,
    Instant endTime,
    String resourceId,
    String resourceType,
    String accountId,
    String region,
    Integer downsampleFactor,
    String rollupStatistic
) {}