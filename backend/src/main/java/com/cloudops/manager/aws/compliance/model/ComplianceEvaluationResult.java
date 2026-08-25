package com.cloudops.manager.aws.compliance.model;

import java.util.List;

public record ComplianceEvaluationResult(
    String ruleId,
    ComplianceCategory category,
    ComplianceStatus status,
    String title,
    String explanation,
    List<ComplianceEvidence> evidence
) {}