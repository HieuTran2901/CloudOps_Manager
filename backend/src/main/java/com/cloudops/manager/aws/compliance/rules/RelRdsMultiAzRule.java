package com.cloudops.manager.aws.compliance.rules;

import com.cloudops.manager.aws.compliance.model.*;
import com.cloudops.manager.aws.discovery.model.RdsDetailResource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RelRdsMultiAzRule implements ComplianceRule {

    @Override
    public String ruleId() {
        return "REL-RDS-001";
    }

    @Override
    public ComplianceCategory category() {
        return ComplianceCategory.RELIABILITY;
    }

    @Override
    public String title() {
        return "RDS Multi-AZ Deployment";
    }

    @Override
    public String description() {
        return "Verifies that RDS database instances have Multi-AZ configured for high availability.";
    }

    @Override
    public ComplianceEvaluationResult evaluate(ComplianceEvaluationContext context) {
        if (context == null || context.rdsDatabases() == null) {
            return new ComplianceEvaluationResult(ruleId(), category(), ComplianceStatus.INSUFFICIENT_EVIDENCE, title(), "RDS database evidence is unavailable.", List.of());
        }

        if (context.rdsDatabases().isEmpty()) {
            return new ComplianceEvaluationResult(ruleId(), category(), ComplianceStatus.NOT_APPLICABLE, title(), "No RDS database instances found in scope.", List.of());
        }

        List<ComplianceEvidence> failingEvidence = new ArrayList<>();
        for (RdsDetailResource db : context.rdsDatabases()) {
            if (db.multiAz() == null || !db.multiAz()) {
                failingEvidence.add(new ComplianceEvidence(
                        "AWS::RDS::DBInstance",
                        db.dbInstanceIdentifier(),
                        Map.of("dbInstanceIdentifier", db.dbInstanceIdentifier(), "multiAz", false)
                ));
            }
        }

        if (!failingEvidence.isEmpty()) {
            return new ComplianceEvaluationResult(
                    ruleId(),
                    category(),
                    ComplianceStatus.FAIL,
                    title(),
                    failingEvidence.size() + " RDS instance(s) do not have Multi-AZ enabled.",
                    failingEvidence
            );
        }

        return new ComplianceEvaluationResult(
                ruleId(),
                category(),
                ComplianceStatus.PASS,
                title(),
                "All " + context.rdsDatabases().size() + " RDS instance(s) have Multi-AZ enabled.",
                List.of()
        );
    }
}