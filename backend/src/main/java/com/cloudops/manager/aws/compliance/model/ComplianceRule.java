package com.cloudops.manager.aws.compliance.model;

public interface ComplianceRule {
    String ruleId();
    ComplianceCategory category();
    String title();
    String description();
    ComplianceEvaluationResult evaluate(ComplianceEvaluationContext context);
}