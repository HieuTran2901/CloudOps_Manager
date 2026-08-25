package com.cloudops.manager.aws.compliance.rules;

import com.cloudops.manager.aws.compliance.model.*;
import com.cloudops.manager.aws.discovery.model.SecurityGroupDetailResource;
import com.cloudops.manager.aws.discovery.model.SecurityGroupRuleDetail;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SecSgOpenIngressRule implements ComplianceRule {

    private static final List<Integer> SENSITIVE_PORTS = List.of(22, 3389);

    @Override
    public String ruleId() {
        return "SEC-SG-001";
    }

    @Override
    public ComplianceCategory category() {
        return ComplianceCategory.SECURITY;
    }

    @Override
    public String title() {
        return "Security Groups Restricted Administrative Ingress";
    }

    @Override
    public String description() {
        return "Verifies that security groups do not permit unrestricted ingress (0.0.0.0/0) on administrative ports (22, 3389).";
    }

    @Override
    public ComplianceEvaluationResult evaluate(ComplianceEvaluationContext context) {
        if (context == null || context.securityGroups() == null) {
            return new ComplianceEvaluationResult(ruleId(), category(), ComplianceStatus.INSUFFICIENT_EVIDENCE, title(), "Security group evidence is unavailable.", List.of());
        }

        if (context.securityGroups().isEmpty()) {
            return new ComplianceEvaluationResult(ruleId(), category(), ComplianceStatus.NOT_APPLICABLE, title(), "No security groups found in scope.", List.of());
        }

        List<ComplianceEvidence> failingEvidence = new ArrayList<>();
        for (SecurityGroupDetailResource sg : context.securityGroups()) {
            if (sg.inboundRules() != null) {
                for (SecurityGroupRuleDetail rule : sg.inboundRules()) {
                    if (rule.ipv4Cidrs() != null && rule.ipv4Cidrs().contains("0.0.0.0/0") && coversSensitivePort(rule)) {
                        failingEvidence.add(new ComplianceEvidence(
                                "AWS::EC2::SecurityGroup",
                                sg.securityGroupId(),
                                Map.of("securityGroupId", sg.securityGroupId(), "securityGroupName", sg.securityGroupName() != null ? sg.securityGroupName() : "", "cidr", "0.0.0.0/0",
                                        "fromPort", rule.fromPort() != null ? rule.fromPort() : "all",
                                        "toPort", rule.toPort() != null ? rule.toPort() : "all")
                        ));
                    }
                }
            }
        }

        if (!failingEvidence.isEmpty()) {
            return new ComplianceEvaluationResult(
                    ruleId(),
                    category(),
                    ComplianceStatus.FAIL,
                    title(),
                    failingEvidence.size() + " security group rule(s) allow unrestricted 0.0.0.0/0 ingress on administrative ports.",
                    failingEvidence
            );
        }

        return new ComplianceEvaluationResult(
                ruleId(),
                category(),
                ComplianceStatus.PASS,
                title(),
                "All security groups restrict administrative ingress ports.",
                List.of()
        );
    }

    private boolean coversSensitivePort(SecurityGroupRuleDetail rule) {
        if (rule.fromPort() == null && rule.toPort() == null) return true; // all ports
        int from = rule.fromPort() != null ? rule.fromPort() : 0;
        int to = rule.toPort() != null ? rule.toPort() : 65535;
        for (int port : SENSITIVE_PORTS) {
            if (port >= from && port <= to) return true;
        }
        return false;
    }
}