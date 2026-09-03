package com.cloudops.manager.aws.quota.model;

/**
 * Deterministic status based on quota utilization percentage:
 * - NORMAL: known utilization < 80.0%
 * - WARNING: 80.0% <= known utilization < 90.0%
 * - CRITICAL: known utilization >= 90.0%
 * - UNKNOWN: usage cannot be determined or missing limit
 */
public enum QuotaStatus {
    NORMAL,
    WARNING,
    CRITICAL,
    UNKNOWN
}
