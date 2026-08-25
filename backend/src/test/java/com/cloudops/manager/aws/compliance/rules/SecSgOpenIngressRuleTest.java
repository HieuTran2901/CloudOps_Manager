package com.cloudops.manager.aws.compliance.rules;

import com.cloudops.manager.aws.compliance.model.*;
import com.cloudops.manager.aws.discovery.model.SecurityGroupDetailResource;
import com.cloudops.manager.aws.discovery.model.SecurityGroupRuleDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecSgOpenIngressRuleTest {

    private final SecSgOpenIngressRule rule = new SecSgOpenIngressRule();

    @Test
    @DisplayName("Should return FAIL when Security Group allows 0.0.0.0/0 on port 22")
    void shouldFailOnOpenSsh() {
        SecurityGroupRuleDetail openSsh = new SecurityGroupRuleDetail(
                "tcp", 22, 22, List.of("0.0.0.0/0"), List.of(), List.of(), List.of(), "Open SSH"
        );
        SecurityGroupDetailResource sg = new SecurityGroupDetailResource(
                "sg-123", "arn:aws:ec2:us-east-1:123:sg/sg-123", "open-sg", "Test SG", "vpc-1",
                "123456789012", "123456789012", "us-east-1",
                List.of(openSsh), List.of(), Map.of(), Instant.now()
        );

        ComplianceEvaluationContext ctx = new ComplianceEvaluationContext(
                "123456789012", "us-east-1", List.of(), List.of(), List.of(sg), List.of(), List.of(), Map.of()
        );

        ComplianceEvaluationResult res = rule.evaluate(ctx);
        assertThat(res.status()).isEqualTo(ComplianceStatus.FAIL);
        assertThat(res.evidence()).hasSize(1);
    }

    @Test
    @DisplayName("Should return PASS when Security Group does not allow open administrative ports")
    void shouldPassOnRestrictedIngress() {
        SecurityGroupRuleDetail restricted = new SecurityGroupRuleDetail(
                "tcp", 80, 80, List.of("0.0.0.0/0"), List.of(), List.of(), List.of(), "HTTP"
        );
        SecurityGroupDetailResource sg = new SecurityGroupDetailResource(
                "sg-123", "arn:aws:ec2:us-east-1:123:sg/sg-123", "web-sg", "Test SG", "vpc-1",
                "123456789012", "123456789012", "us-east-1",
                List.of(restricted), List.of(), Map.of(), Instant.now()
        );

        ComplianceEvaluationContext ctx = new ComplianceEvaluationContext(
                "123456789012", "us-east-1", List.of(), List.of(), List.of(sg), List.of(), List.of(), Map.of()
        );

        ComplianceEvaluationResult res = rule.evaluate(ctx);
        assertThat(res.status()).isEqualTo(ComplianceStatus.PASS);
    }
}