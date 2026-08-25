package com.cloudops.manager.aws.cost.model;

import java.math.BigDecimal;
import java.util.Map;

public record CostDataPoint(
    CostTimePeriod timePeriod,
    BigDecimal amount,
    String unit,
    String metric,
    Map<String, String> dimensions
) {}