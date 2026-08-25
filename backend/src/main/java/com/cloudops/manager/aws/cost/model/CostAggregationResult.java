package com.cloudops.manager.aws.cost.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CostAggregationResult(
    String accountId,
    String billingScope,
    String metric,
    String granularity,
    CostTimePeriod timePeriod,
    BigDecimal totalAmount,
    String unit,
    List<CostPeriodResult> resultsByTime,
    Instant queriedAt
) {}