package com.cloudops.manager.aws.observability.model;

import java.time.Instant;

public record MetricDataPoint(
    Instant timestamp,
    Double value,
    String unit,
    String statistic
) {}