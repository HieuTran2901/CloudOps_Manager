package com.cloudops.manager.operations.risk.model;

import java.time.Instant;
import java.util.List;

/**
 * Aggregated operational risk assessment report.
 */
public record RiskAssessmentReport(
    String accountId,
    String region,
    int totalRisksTracked,
    int criticalCount,
    int highCount,
    int mediumCount,
    int lowCount,
    List<OperationalRisk> risks,
    Instant generatedAt
) {}
