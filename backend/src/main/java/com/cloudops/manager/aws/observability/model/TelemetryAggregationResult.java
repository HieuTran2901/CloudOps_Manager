package com.cloudops.manager.aws.observability.model;

import java.time.Instant;
import java.util.List;

public record TelemetryAggregationResult(
    String accountId,
    String region,
    Instant startTime,
    Instant endTime,
    int totalMetrics,
    int totalDatapoints,
    List<MetricSeries> series,
    Instant queriedAt
) {}