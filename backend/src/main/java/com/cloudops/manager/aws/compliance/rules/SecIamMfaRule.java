package com.cloudops.manager.aws.compliance.rules;

import com.cloudops.manager.aws.compliance.model.*;
import com.cloudops.manager.aws.discovery.model.IamUserDetailResource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SecIamMfaRule implements ComplianceRule {

    @Override
    public String ruleId() {
        return "SEC-IAM-001";
    }

    @Override
    public ComplianceCategory category() {
        return ComplianceCategory.SECURITY;
    }

    @Override
    public String title() {
        return "IAM Users MFA Enabled";
    }

    @Override
    public String description() {
        return "Verifies that all IAM users have Multi-Factor Authentication (MFA) enabled.";
    }

    @Override
    public ComplianceEvaluationResult evaluate(ComplianceEvaluationContext context) {
        if (context == null || context.iamUsers() == null) {
            return new ComplianceEvaluationResult(ruleId(), category(), ComplianceStatus.INSUFFICIENT_EVIDENCE, title(), "IAM user evidence is unavailable.", List.of());
        }

        if (context.iamUsers().isEmpty()) {
            return new ComplianceEvaluationResult(ruleId(), category(), ComplianceStatus.NOT_APPLICABLE, title(), "No IAM users found in account.", List.of());
        }

        List<ComplianceEvidence> failingEvidence = new ArrayList<>();
        for (IamUserDetailResource user : context.iamUsers()) {
            if (user.mfaDevices() == null || user.mfaDevices().isEmpty()) {
                failingEvidence.add(new ComplianceEvidence(
                        "AWS::IAM::User",
                        user.userName(),
                        Map.of("userName", user.userName(), "mfaEnabled", false)
                ));
            }
        }

        if (!failingEvidence.isEmpty()) {
            return new ComplianceEvaluationResult(
                    ruleId(),
                    category(),
                    ComplianceStatus.FAIL,
                    title(),
                    failingEvidence.size() + " IAM user(s) do not have MFA enabled.",
                    failingEvidence
            );
        }

        return new ComplianceEvaluationResult(
                ruleId(),
                category(),
                ComplianceStatus.PASS,
                title(),
                "All " + context.iamUsers().size() + " IAM user(s) have MFA enabled.",
                List.of()
        );
    }
}