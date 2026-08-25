package com.cloudops.manager.aws.observability.model;

import java.time.Instant;

public record MetricQuery(
    String metricName,
    String instanceId,
    String region,
    String accountId,
    Instant startTime,
    Instant endTime,
    int periodSeconds,
    String statistic
) {}