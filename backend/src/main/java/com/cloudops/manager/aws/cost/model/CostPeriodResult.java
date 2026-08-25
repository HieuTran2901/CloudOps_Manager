package com.cloudops.manager.aws.cost.model;

import java.math.BigDecimal;
import java.util.List;

public record CostPeriodResult(
    CostTimePeriod timePeriod,
    BigDecimal totalAmount,
    String unit,
    List<CostGroup> groups
) {}