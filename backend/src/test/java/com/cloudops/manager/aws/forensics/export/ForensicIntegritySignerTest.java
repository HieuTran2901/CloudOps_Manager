package com.cloudops.manager.aws.forensics.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ForensicIntegritySignerTest {

    private final ForensicIntegritySigner signer = new ForensicIntegritySigner();

    @Test
    @DisplayName("Should compute accurate deterministic SHA-256 digest")
    void shouldComputeSha256() {
        String content = "CloudOpsManagerForensicEvidence";
        String digest1 = signer.computeSha256(content);
        String digest2 = signer.computeSha256(content);

        assertThat(digest1).isNotBlank();
        assertThat(digest1).isEqualTo(digest2);
        assertThat(digest1).hasSize(64);
    }
}