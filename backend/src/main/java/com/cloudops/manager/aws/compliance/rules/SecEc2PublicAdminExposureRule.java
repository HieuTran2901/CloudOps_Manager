package com.cloudops.manager.aws.compliance.rules;

import com.cloudops.manager.aws.compliance.model.*;
import com.cloudops.manager.aws.discovery.model.Ec2DetailResource;
import com.cloudops.manager.aws.discovery.model.Ec2NetworkInterfaceDetail;
import com.cloudops.manager.aws.discovery.model.SecurityGroupDetailResource;
import com.cloudops.manager.aws.discovery.model.SecurityGroupRuleDetail;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SecEc2PublicAdminExposureRule implements ComplianceRule {

    private static final List<Integer> SENSITIVE_PORTS = List.of(22, 3389);

    @Override
    public String ruleId() {
        return "SEC-EC2-001";
    }

    @Override
    public ComplianceCategory category() {
        return ComplianceCategory.SECURITY;
    }

    @Override
    public String title() {
        return "Publicly Exposed Administrative EC2 Instances";
    }

    @Override
    public String description() {
        return "Verifies that running EC2 instances with public IPv4 addresses are not associated with security groups permitting unrestricted (0.0.0.0/0) ingress on administrative ports (22, 3389).";
    }

    @Override
    public ComplianceEvaluationResult evaluate(ComplianceEvaluationContext context) {
        if (context == null || context.ec2Instances() == null || context.securityGroups() == null) {
            return new ComplianceEvaluationResult(ruleId(), category(), ComplianceStatus.INSUFFICIENT_EVIDENCE, title(), "EC2 instance or Security Group evidence is unavailable.", List.of());
        }

        if (context.ec2Instances().isEmpty()) {
            return new ComplianceEvaluationResult(ruleId(), category(), ComplianceStatus.NOT_APPLICABLE, title(), "No EC2 instances found in scope.", List.of());
        }

        Map<String, SecurityGroupDetailResource> sgMap = new HashMap<>();
        for (SecurityGroupDetailResource sg : context.securityGroups()) {
            sgMap.put(sg.securityGroupId(), sg);
        }

        List<ComplianceEvidence> failingEvidence = new ArrayList<>();
        int runningPublicInstances = 0;

        for (Ec2DetailResource ec2 : context.ec2Instances()) {
            boolean isRunning = "running".equalsIgnoreCase(ec2.instanceState());
            String publicIp = ec2.publicIp();

            if (isRunning && publicIp != null && !publicIp.isBlank()) {
                runningPublicInstances++;
                Set<String> attachedSgIds = extractSecurityGroupIds(ec2);

                for (String sgId : attachedSgIds) {
                    SecurityGroupDetailResource sg = sgMap.get(sgId);
                    if (sg != null && sg.inboundRules() != null) {
                        for (SecurityGroupRuleDetail rule : sg.inboundRules()) {
                            if (rule.ipv4Cidrs() != null && rule.ipv4Cidrs().contains("0.0.0.0/0") && coversSensitivePort(rule)) {
                                failingEvidence.add(new ComplianceEvidence(
                                        "AWS::EC2::Instance",
                                        ec2.instanceId(),
                                        Map.of(
                                                "instanceId", ec2.instanceId(),
                                                "instanceState", ec2.instanceState(),
                                                "publicIp", publicIp,
                                                "exposedSecurityGroupId", sgId,
                                                "fromPort", rule.fromPort() != null ? rule.fromPort() : "all",
                                                "toPort", rule.toPort() != null ? rule.toPort() : "all",
                                                "cidr", "0.0.0.0/0"
                                        )
                                ));
                            }
                        }
                    }
                }
            }
        }

        if (runningPublicInstances == 0) {
            return new ComplianceEvaluationResult(ruleId(), category(), ComplianceStatus.PASS, title(), "No running EC2 instances with public IPv4 addresses found.", List.of());
        }

        if (!failingEvidence.isEmpty()) {
            return new ComplianceEvaluationResult(
                    ruleId(),
                    category(),
                    ComplianceStatus.FAIL,
                    title(),
                    failingEvidence.size() + " running EC2 instance exposure(s) detected with unrestricted administrative ingress.",
                    failingEvidence
            );
        }

        return new ComplianceEvaluationResult(
                ruleId(),
                category(),
                ComplianceStatus.PASS,
                title(),
                "All " + runningPublicInstances + " running public EC2 instance(s) have restricted administrative ports.",
                List.of()
        );
    }

    private Set<String> extractSecurityGroupIds(Ec2DetailResource ec2) {
        Set<String> set = new HashSet<>();
        if (ec2.networkInterfaces() != null) {
            for (Ec2NetworkInterfaceDetail eni : ec2.networkInterfaces()) {
                if (eni.securityGroupIds() != null) {
                    set.addAll(eni.securityGroupIds());
                }
            }
        }
        return set;
    }

    private boolean coversSensitivePort(SecurityGroupRuleDetail rule) {
        if (rule.fromPort() == null && rule.toPort() == null) return true;
        int from = rule.fromPort() != null ? rule.fromPort() : 0;
        int to = rule.toPort() != null ? rule.toPort() : 65535;
        for (int port : SENSITIVE_PORTS) {
            if (port >= from && port <= to) return true;
        }
        return false;
    }
}