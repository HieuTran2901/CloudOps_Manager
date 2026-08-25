package com.cloudops.manager.aws.compliance.rules;

import com.cloudops.manager.aws.compliance.model.*;
import com.cloudops.manager.aws.discovery.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecEc2PublicAdminExposureRuleTest {

    private final SecEc2PublicAdminExposureRule rule = new SecEc2PublicAdminExposureRule();

    @Test
    @DisplayName("Should FAIL when running EC2 instance has public IP and SG allows 0.0.0.0/0 on port 22")
    void shouldFailOnExposedAdminPort() {
        Ec2NetworkInterfaceDetail eni = new Ec2NetworkInterfaceDetail(
                "eni-1", "sub-1", "vpc-1", "10.0.0.1", List.of("10.0.0.1"), "54.200.10.5",
                "00:11:22:33:44:55", List.of("sg-open"), List.of("open-sg"), "in-use", "", "interface"
        );

        Ec2DetailResource ec2 = new Ec2DetailResource(
                "i-123", "web-server", "arn:aws:ec2:us-east-1:123:instance/i-123", "123456789012", "us-east-1",
                "t3.micro", "x86_64", "Linux", "Linux/UNIX", "ami-1", null, Instant.now(),
                "running", null, null, "disabled", "us-east-1a", null, "default", "vpc-1", "sub-1",
                "10.0.0.1", "54.200.10.5", "ip-10-0-0-1", "ec2-54-200-10-5", List.of(), List.of(eni), Map.of(), Instant.now()
        );

        SecurityGroupRuleDetail openRule = new SecurityGroupRuleDetail(
                "tcp", 22, 22, List.of("0.0.0.0/0"), List.of(), List.of(), List.of(), "SSH"
        );
        SecurityGroupDetailResource sg = new SecurityGroupDetailResource(
                "sg-open", "arn:aws:ec2:us-east-1:123:sg/sg-open", "open-sg", "Desc", "vpc-1",
                "123456789012", "123456789012", "us-east-1", List.of(openRule), List.of(), Map.of(), Instant.now()
        );

        ComplianceEvaluationContext ctx = new ComplianceEvaluationContext(
                "123456789012", "us-east-1", List.of(), List.of(), List.of(sg), List.of(), List.of(),
                List.of(ec2), List.of(), CorrelatedEvidenceSet.empty(), Map.of(), Map.of()
        );

        ComplianceEvaluationResult res = rule.evaluate(ctx);
        assertThat(res.status()).isEqualTo(ComplianceStatus.FAIL);
        assertThat(res.evidence()).hasSize(1);
        assertThat(res.evidence().get(0).resourceId()).isEqualTo("i-123");
    }

    @Test
    @DisplayName("Should PASS when running EC2 instance has public IP but restricted SG")
    void shouldPassOnRestrictedAdminPort() {
        Ec2NetworkInterfaceDetail eni = new Ec2NetworkInterfaceDetail(
                "eni-1", "sub-1", "vpc-1", "10.0.0.1", List.of("10.0.0.1"), "54.200.10.5",
                "00:11:22:33:44:55", List.of("sg-safe"), List.of("safe-sg"), "in-use", "", "interface"
        );

        Ec2DetailResource ec2 = new Ec2DetailResource(
                "i-123", "web-server", "arn:aws:ec2:us-east-1:123:instance/i-123", "123456789012", "us-east-1",
                "t3.micro", "x86_64", "Linux", "Linux/UNIX", "ami-1", null, Instant.now(),
                "running", null, null, "disabled", "us-east-1a", null, "default", "vpc-1", "sub-1",
                "10.0.0.1", "54.200.10.5", "ip-10-0-0-1", "ec2-54-200-10-5", List.of(), List.of(eni), Map.of(), Instant.now()
        );

        SecurityGroupRuleDetail safeRule = new SecurityGroupRuleDetail(
                "tcp", 443, 443, List.of("0.0.0.0/0"), List.of(), List.of(), List.of(), "HTTPS"
        );
        SecurityGroupDetailResource sg = new SecurityGroupDetailResource(
                "sg-safe", "arn:aws:ec2:us-east-1:123:sg/sg-safe", "safe-sg", "Desc", "vpc-1",
                "123456789012", "123456789012", "us-east-1", List.of(safeRule), List.of(), Map.of(), Instant.now()
        );

        ComplianceEvaluationContext ctx = new ComplianceEvaluationContext(
                "123456789012", "us-east-1", List.of(), List.of(), List.of(sg), List.of(), List.of(),
                List.of(ec2), List.of(), CorrelatedEvidenceSet.empty(), Map.of(), Map.of()
        );

        ComplianceEvaluationResult res = rule.evaluate(ctx);
        assertThat(res.status()).isEqualTo(ComplianceStatus.PASS);
    }
}