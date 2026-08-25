package com.cloudops.manager.aws.compliance.rules;

import com.cloudops.manager.aws.audit.model.CloudTrailEventIdentity;
import com.cloudops.manager.aws.audit.model.CloudTrailEventResource;
import com.cloudops.manager.aws.audit.model.CloudTrailEventResourceReference;
import com.cloudops.manager.aws.compliance.model.*;
import com.cloudops.manager.aws.discovery.model.Ec2DetailResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecEc2AdminActivityCorrelationRuleTest {

    private final SecEc2AdminActivityCorrelationRule rule = new SecEc2AdminActivityCorrelationRule();

    @Test
    @DisplayName("Should correlate CloudTrail events with publicly exposed EC2 instances")
    void shouldCorrelateCloudTrailEvents() {
        Ec2DetailResource ec2 = new Ec2DetailResource(
                "i-exposed", "app-server", "arn:aws:ec2:us-east-1:123:instance/i-exposed", "123456789012", "us-east-1",
                "t3.micro", "x86_64", "Linux", "Linux", "ami-1", null, Instant.now(),
                "running", null, null, "disabled", "us-east-1a", null, "default", "vpc-1", "sub-1",
                "10.0.0.2", "54.200.10.10", "dns", "dns", List.of(), List.of(), Map.of(), Instant.now()
        );

        CloudTrailEventResourceReference ref = new CloudTrailEventResourceReference("AWS::EC2::Instance", "i-exposed");
        CloudTrailEventResource ctEvent = new CloudTrailEventResource(
                "ev-101", "StopInstances", "ec2.amazonaws.com", Instant.now(), "us-east-1",
                new CloudTrailEventIdentity("usr-1", "alice", "123456789012", "IAMUser"),
                "1.2.3.4", "AWS-CLI", List.of(ref), false, "AKIA1", "Management"
        );

        ComplianceEvaluationContext ctx = new ComplianceEvaluationContext(
                "123456789012", "us-east-1", List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(ec2), List.of(ctEvent), CorrelatedEvidenceSet.empty(), Map.of(), Map.of()
        );

        ComplianceEvaluationResult res = rule.evaluate(ctx);
        assertThat(res.status()).isEqualTo(ComplianceStatus.FAIL);
        assertThat(res.evidence()).hasSize(1);
        assertThat(res.evidence().get(0).resourceId()).isEqualTo("i-exposed");
    }
}