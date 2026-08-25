package com.cloudops.manager.aws.compliance.rules;

import com.cloudops.manager.aws.audit.model.CloudTrailEventResource;
import com.cloudops.manager.aws.audit.model.CloudTrailEventResourceReference;
import com.cloudops.manager.aws.compliance.model.*;
import com.cloudops.manager.aws.discovery.model.Ec2DetailResource;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SecEc2AdminActivityCorrelationRule implements ComplianceRule {

    @Override
    public String ruleId() {
        return "SEC-EC2-002";
    }

    @Override
    public ComplianceCategory category() {
        return ComplianceCategory.SECURITY;
    }

    @Override
    public String title() {
        return "Administrative Activity on Publicly Exposed EC2 Instances";
    }

    @Override
    public String description() {
        return "Correlates observed CloudTrail operational management events against running EC2 instances with public IPv4 addresses.";
    }

    @Override
    public ComplianceEvaluationResult evaluate(ComplianceEvaluationContext context) {
        if (context == null || context.ec2Instances() == null || context.recentAuditEvents() == null) {
            return new ComplianceEvaluationResult(ruleId(), category(), ComplianceStatus.INSUFFICIENT_EVIDENCE, title(), "EC2 instance or CloudTrail audit evidence is unavailable.", List.of());
        }

        List<Ec2DetailResource> publicInstances = new ArrayList<>();
        for (Ec2DetailResource ec2 : context.ec2Instances()) {
            if ("running".equalsIgnoreCase(ec2.instanceState()) && ec2.publicIp() != null && !ec2.publicIp().isBlank()) {
                publicInstances.add(ec2);
            }
        }

        if (publicInstances.isEmpty()) {
            return new ComplianceEvaluationResult(ruleId(), category(), ComplianceStatus.NOT_APPLICABLE, title(), "No running public EC2 instances found in scope.", List.of());
        }

        List<ComplianceEvidence> correlatedEvidence = new ArrayList<>();
        for (Ec2DetailResource ec2 : publicInstances) {
            for (CloudTrailEventResource event : context.recentAuditEvents()) {
                if (isEventTargetingInstance(event, ec2.instanceId())) {
                    correlatedEvidence.add(new ComplianceEvidence(
                            "AWS::EC2::Instance",
                            ec2.instanceId(),
                            Map.of(
                                    "instanceId", ec2.instanceId(),
                                    "publicIp", ec2.publicIp(),
                                    "eventId", event.eventId(),
                                    "eventName", event.eventName(),
                                    "eventTime", event.eventTime() != null ? event.eventTime().toString() : "",
                                    "username", (event.userIdentity() != null && event.userIdentity().username() != null) ? event.userIdentity().username() : "unknown"
                            )
                    ));
                }
            }
        }

        if (!correlatedEvidence.isEmpty()) {
            return new ComplianceEvaluationResult(
                    ruleId(),
                    category(),
                    ComplianceStatus.FAIL,
                    title(),
                    correlatedEvidence.size() + " operational event(s) observed targeting publicly exposed EC2 instances.",
                    correlatedEvidence
            );
        }

        return new ComplianceEvaluationResult(
                ruleId(),
                category(),
                ComplianceStatus.PASS,
                title(),
                "No operational management events observed targeting " + publicInstances.size() + " public EC2 instance(s).",
                List.of()
        );
    }

    private boolean isEventTargetingInstance(CloudTrailEventResource event, String instanceId) {
        if (event.resources() != null) {
            for (CloudTrailEventResourceReference ref : event.resources()) {
                if (instanceId.equalsIgnoreCase(ref.resourceName())) {
                    return true;
                }
            }
        }
        return false;
    }
}