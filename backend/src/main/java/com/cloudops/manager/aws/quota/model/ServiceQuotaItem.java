package com.cloudops.manager.aws.quota.model;

import java.time.Instant;

/**
 * Domain model representing an individual AWS Service Quota along with correlated usage.
 */
public record ServiceQuotaItem(
    String serviceCode,
    String serviceName,
    String quotaCode,
    String quotaName,
    Double appliedLimit,
    Double currentUsage,
    Double utilizationPercentage,
    QuotaStatus status,
    String region,
    String usageSource,
    String unit,
    boolean adjustable,
    Instant evaluatedAt
) {}
