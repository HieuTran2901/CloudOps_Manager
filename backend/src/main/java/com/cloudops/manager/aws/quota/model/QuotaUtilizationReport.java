package com.cloudops.manager.aws.quota.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Aggregated report of AWS Service Quotas with utilization and exhaustion metrics.
 */
public record QuotaUtilizationReport(
    String accountId,
    String region,
    int totalQuotasTracked,
    int normalCount,
    int warningCount,
    int criticalCount,
    int unknownCount,
    double highestUtilizationPercentage,
    List<ServiceQuotaItem> quotas,
    Map<String, Integer> statusSummary,
    Instant generatedAt
) {}
