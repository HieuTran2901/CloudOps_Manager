package com.cloudops.manager.aws.compliance.model;

import java.time.Instant;
import java.util.List;

public record ComplianceEvaluationReport(
    String accountId,
    String region,
    Instant evaluatedAt,
    int totalRulesEvaluated,
    int passCount,
    int failCount,
    int notApplicableCount,
    int insufficientEvidenceCount,
    List<ComplianceEvaluationResult> results
) {}