package com.cloudops.manager.aws.sts.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AwsAccountTargetTest {

    @Test
    @DisplayName("Should create valid AwsAccountTarget with default session name")
    void shouldCreateValidAccountTarget() {
        AwsAccountTarget target = new AwsAccountTarget(
                "123456789012",
                "arn:aws:iam::123456789012:role/CloudOpsAuditRole",
                null,
                "ext-123",
                "us-east-1"
        );

        assertThat(target.accountId()).isEqualTo("123456789012");
        assertThat(target.roleArn()).isEqualTo("arn:aws:iam::123456789012:role/CloudOpsAuditRole");
        assertThat(target.roleSessionName()).startsWith("cloudops-discovery-");
        assertThat(target.externalId()).isEqualTo("ext-123");
        assertThat(target.region()).isEqualTo("us-east-1");
    }

    @Test
    @DisplayName("Should reject invalid account ID format")
    void shouldRejectInvalidAccountId() {
        assertThatThrownBy(() -> new AwsAccountTarget("12345", "arn:aws:iam::123456789012:role/Role", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("12 digits");
    }

    @Test
    @DisplayName("Should reject invalid role ARN format")
    void shouldRejectInvalidRoleArn() {
        assertThatThrownBy(() -> new AwsAccountTarget("123456789012", "invalid-role-arn", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IAM Role ARN");
    }

    @Test
    @DisplayName("Should reject role ARN account mismatch")
    void shouldRejectRoleArnAccountMismatch() {
        assertThatThrownBy(() -> new AwsAccountTarget("111111111111", "arn:aws:iam::222222222222:role/Role", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match target account ID");
    }
}