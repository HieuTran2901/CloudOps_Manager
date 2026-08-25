package com.cloudops.manager.aws.compliance.rules;

import com.cloudops.manager.aws.compliance.model.*;
import com.cloudops.manager.aws.discovery.model.S3DetailResource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SecS3PublicAccessBlockRule implements ComplianceRule {

    @Override
    public String ruleId() {
        return "SEC-S3-001";
    }

    @Override
    public ComplianceCategory category() {
        return ComplianceCategory.SECURITY;
    }

    @Override
    public String title() {
        return "S3 Bucket Public Access Block";
    }

    @Override
    public String description() {
        return "Verifies that S3 buckets have Public Access Block enabled.";
    }

    @Override
    public ComplianceEvaluationResult evaluate(ComplianceEvaluationContext context) {
        if (context == null || context.s3Buckets() == null) {
            return new ComplianceEvaluationResult(ruleId(), category(), ComplianceStatus.INSUFFICIENT_EVIDENCE, title(), "S3 bucket evidence is unavailable.", List.of());
        }

        if (context.s3Buckets().isEmpty()) {
            return new ComplianceEvaluationResult(ruleId(), category(), ComplianceStatus.NOT_APPLICABLE, title(), "No S3 buckets found in scope.", List.of());
        }

        List<ComplianceEvidence> failingEvidence = new ArrayList<>();
        for (S3DetailResource bucket : context.s3Buckets()) {
            if (bucket.publicAccessBlock() == null ||
                !Boolean.TRUE.equals(bucket.publicAccessBlock().blockPublicAcls()) ||
                !Boolean.TRUE.equals(bucket.publicAccessBlock().blockPublicPolicy())) {
                failingEvidence.add(new ComplianceEvidence(
                        "AWS::S3::Bucket",
                        bucket.bucketName(),
                        Map.of("bucketName", bucket.bucketName(), "publicAccessBlockConfigured", bucket.publicAccessBlock() != null)
                ));
            }
        }

        if (!failingEvidence.isEmpty()) {
            return new ComplianceEvaluationResult(
                    ruleId(),
                    category(),
                    ComplianceStatus.FAIL,
                    title(),
                    failingEvidence.size() + " S3 bucket(s) do not have full Public Access Block enabled.",
                    failingEvidence
            );
        }

        return new ComplianceEvaluationResult(
                ruleId(),
                category(),
                ComplianceStatus.PASS,
                title(),
                "All " + context.s3Buckets().size() + " S3 bucket(s) have Public Access Block enabled.",
                List.of()
        );
    }
}