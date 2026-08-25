package com.cloudops.manager.aws.cost.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record CostQueryRequest(
    String accountId,
    String metric,
    String granularity,
    LocalDate startDate,
    LocalDate endDate,
    List<String> groupByDimensions,
    Map<String, List<String>> dimensionFilters
) {}