package com.cloudops.manager.aws.compliance.model;

public record ComplianceRuleDefinition(
    String ruleId,
    ComplianceCategory category,
    String title,
    String description
) {
    public static ComplianceRuleDefinition fromRule(ComplianceRule rule) {
        return new ComplianceRuleDefinition(rule.ruleId(), rule.category(), rule.title(), rule.description());
    }
}