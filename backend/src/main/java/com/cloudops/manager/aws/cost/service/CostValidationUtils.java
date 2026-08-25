package com.cloudops.manager.aws.cost.service;

import com.cloudops.manager.aws.cost.model.CostQueryRequest;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;

public final class CostValidationUtils {

    public static final Set<String> ALLOWED_METRICS = Set.of(
            "UnblendedCost", "AmortizedCost", "NetUnblendedCost", "NetAmortizedCost", "UsageQuantity"
    );

    public static final Set<String> ALLOWED_GRANULARITIES = Set.of("DAILY", "MONTHLY");
    public static final Set<String> ALLOWED_DIMENSIONS = Set.of("SERVICE", "LINKED_ACCOUNT", "USAGE_TYPE");

    private CostValidationUtils() {}

    public static void validateRequest(CostQueryRequest request) {
        if (request == null) throw new IllegalArgumentException("CostQueryRequest must not be null");

        if (request.metric() == null || !ALLOWED_METRICS.contains(request.metric().trim())) {
            throw new IllegalArgumentException("Invalid metric '" + request.metric() + "'. Allowed: " + ALLOWED_METRICS);
        }

        if (request.granularity() == null || !ALLOWED_GRANULARITIES.contains(request.granularity().trim().toUpperCase())) {
            throw new IllegalArgumentException("Invalid granularity '" + request.granularity() + "'. Allowed: " + ALLOWED_GRANULARITIES);
        }

        LocalDate start = request.startDate();
        LocalDate end = request.endDate();
        if (start == null || end == null) {
            throw new IllegalArgumentException("startDate and endDate must not be null");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("startDate must be strictly before endDate");
        }

        Period diff = Period.between(start, end);
        int totalMonths = diff.getYears() * 12 + diff.getMonths();
        if ("DAILY".equalsIgnoreCase(request.granularity()) && totalMonths > 12) {
            throw new IllegalArgumentException("Daily granularity query cannot exceed 12 months");
        }
        if ("MONTHLY".equalsIgnoreCase(request.granularity()) && totalMonths > 36) {
            throw new IllegalArgumentException("Monthly granularity query cannot exceed 36 months");
        }

        List<String> groupBy = request.groupByDimensions();
        if (groupBy != null && !groupBy.isEmpty()) {
            if (groupBy.size() > 2) {
                throw new IllegalArgumentException("Cost Explorer supports at most 2 GroupBy dimensions");
            }
            for (String dim : groupBy) {
                if (dim == null || !ALLOWED_DIMENSIONS.contains(dim.trim().toUpperCase())) {
                    throw new IllegalArgumentException("Unsupported GroupBy dimension '" + dim + "'. Allowed: " + ALLOWED_DIMENSIONS);
                }
            }
        }
    }
}