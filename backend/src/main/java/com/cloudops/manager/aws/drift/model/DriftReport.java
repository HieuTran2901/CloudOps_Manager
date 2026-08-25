package com.cloudops.manager.aws.drift.model;

import java.time.Instant;
import java.util.List;

public record DriftReport(
    String accountId,
    String region,
    Instant evaluatedAt,
    int totalResources,
    int inSyncCount,
    int driftedCount,
    int notFoundCount,
    int unsupportedCount,
    int insufficientEvidenceCount,
    List<DriftResourceResult> resources
) {
    public DriftReport {
        resources = (resources != null) ? List.copyOf(resources) : List.of();
    }
}