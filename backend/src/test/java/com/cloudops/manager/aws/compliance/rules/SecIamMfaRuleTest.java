package com.cloudops.manager.aws.compliance.rules;

import com.cloudops.manager.aws.compliance.model.*;
import com.cloudops.manager.aws.discovery.model.IamMfaDeviceInfo;
import com.cloudops.manager.aws.discovery.model.IamUserDetailResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecIamMfaRuleTest {

    private final SecIamMfaRule rule = new SecIamMfaRule();

    @Test
    @DisplayName("Should return PASS when all users have MFA enabled")
    void shouldPassWhenAllUsersHaveMfa() {
        IamUserDetailResource user = new IamUserDetailResource(
                "alice", "usr-1", "arn:aws:iam::123:user/alice", "/", Instant.now(), "123456789012", true,
                List.of(new IamMfaDeviceInfo("arn:aws:iam::123:mfa/alice", Instant.now())),
                List.of(), List.of(), List.of(), List.of(), Map.of(), Instant.now()
        );

        ComplianceEvaluationContext ctx = new ComplianceEvaluationContext(
                "123456789012", "us-east-1", List.of(), List.of(user), List.of(), List.of(), List.of(), Map.of()
        );

        ComplianceEvaluationResult res = rule.evaluate(ctx);
        assertThat(res.status()).isEqualTo(ComplianceStatus.PASS);
        assertThat(res.ruleId()).isEqualTo("SEC-IAM-001");
    }

    @Test
    @DisplayName("Should return FAIL when user lacks MFA enabled")
    void shouldFailWhenUserLacksMfa() {
        IamUserDetailResource user = new IamUserDetailResource(
                "bob", "usr-2", "arn:aws:iam::123:user/bob", "/", Instant.now(), "123456789012", false,
                List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), Instant.now()
        );

        ComplianceEvaluationContext ctx = new ComplianceEvaluationContext(
                "123456789012", "us-east-1", List.of(), List.of(user), List.of(), List.of(), List.of(), Map.of()
        );

        ComplianceEvaluationResult res = rule.evaluate(ctx);
        assertThat(res.status()).isEqualTo(ComplianceStatus.FAIL);
        assertThat(res.evidence()).hasSize(1);
        assertThat(res.evidence().get(0).resourceId()).isEqualTo("bob");
    }

    @Test
    @DisplayName("Should return INSUFFICIENT_EVIDENCE when user evidence is null")
    void shouldReturnInsufficientEvidenceWhenNull() {
        ComplianceEvaluationContext ctx = new ComplianceEvaluationContext(
                "123456789012", "us-east-1", List.of(), null, List.of(), List.of(), List.of(), Map.of()
        );

        ComplianceEvaluationResult res = rule.evaluate(ctx);
        assertThat(res.status()).isEqualTo(ComplianceStatus.INSUFFICIENT_EVIDENCE);
    }
}