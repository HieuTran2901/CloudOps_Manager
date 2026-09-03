package com.cloudops.manager.operations.risk.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Unified operational risk record correlating cross-domain cloud signals.
 */
public record OperationalRisk(
    String riskId,
    RiskCategory category,
    RiskSeverity severity,
    String title,
    String description,
    String impact,
    List<String> affectedResources,
    Map<String, Object> evidence,
    Instant detectedAt,
    RecommendedAction action,
    RiskSource sourceModule
) {}
