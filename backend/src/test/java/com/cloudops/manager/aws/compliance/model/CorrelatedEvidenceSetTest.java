package com.cloudops.manager.aws.compliance.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelatedEvidenceSetTest {

    @Test
    @DisplayName("Should enforce account and region isolation in EvidenceScope")
    void shouldEnforceScopeIsolation() {
        EvidenceScope scopeA = new EvidenceScope("123456789012", "us-east-1");
        EvidenceScope scopeSame = new EvidenceScope("123456789012", "us-east-1");
        EvidenceScope scopeDiffAccount = new EvidenceScope("999999999999", "us-east-1");
        EvidenceScope scopeDiffRegion = new EvidenceScope("123456789012", "us-west-2");
        EvidenceScope scopeGlobal = new EvidenceScope("123456789012", "global");

        assertThat(scopeA.matches(scopeSame)).isTrue();
        assertThat(scopeA.matches(scopeDiffAccount)).isFalse();
        assertThat(scopeA.matches(scopeDiffRegion)).isFalse();
        assertThat(scopeA.matches(scopeGlobal)).isTrue();
    }

    @Test
    @DisplayName("Should query correlated evidence by resource, type, and scope")
    void shouldQueryCorrelatedEvidence() {
        EvidenceScope scope = new EvidenceScope("123456789012", "us-east-1");
        CorrelatedEvidenceItem item1 = new CorrelatedEvidenceItem(
                "AWS::EC2::Instance", "i-123", scope, "EC2", Map.of("state", "running")
        );
        CorrelatedEvidenceItem item2 = new CorrelatedEvidenceItem(
                "AWS::EC2::SecurityGroup", "sg-123", scope, "VPC", Map.of("port", 22)
        );

        CorrelatedEvidenceSet set = new CorrelatedEvidenceSet(List.of(item1, item2));

        assertThat(set.findByResource("AWS::EC2::Instance", "i-123", scope)).containsExactly(item1);
        assertThat(set.findByType("AWS::EC2::SecurityGroup", scope)).containsExactly(item2);
        assertThat(set.hasEvidence("AWS::EC2::Instance", scope)).isTrue();
        assertThat(set.hasEvidence("AWS::S3::Bucket", scope)).isFalse();
    }
}