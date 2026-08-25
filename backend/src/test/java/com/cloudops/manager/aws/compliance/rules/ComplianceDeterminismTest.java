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

class ComplianceDeterminismTest {

    @Test
    @DisplayName("Evaluating rules repeatedly on identical context must produce identical results")
    void shouldProduceDeterministicResults() {
        SecIamMfaRule rule = new SecIamMfaRule();

        IamUserDetailResource user = new IamUserDetailResource(
                "alice", "usr-1", "arn:aws:iam::123:user/alice", "/", Instant.parse("2026-08-24T00:00:00Z"), "123456789012", true,
                List.of(new IamMfaDeviceInfo("arn:aws:iam::123:mfa/alice", Instant.parse("2026-08-24T00:00:00Z"))),
                List.of(), List.of(), List.of(), List.of(), Map.of(), Instant.parse("2026-08-24T00:00:00Z")
        );

        ComplianceEvaluationContext ctx = new ComplianceEvaluationContext(
                "123456789012", "us-east-1", List.of(), List.of(user), List.of(), List.of(), List.of(), Map.of()
        );

        ComplianceEvaluationResult res1 = rule.evaluate(ctx);
        ComplianceEvaluationResult res2 = rule.evaluate(ctx);

        assertThat(res1.status()).isEqualTo(res2.status());
        assertThat(res1.ruleId()).isEqualTo(res2.ruleId());
        assertThat(res1.explanation()).isEqualTo(res2.explanation());
    }
}