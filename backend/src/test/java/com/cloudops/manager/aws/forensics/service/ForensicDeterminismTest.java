package com.cloudops.manager.aws.forensics.service;

import com.cloudops.manager.aws.forensics.export.ForensicCsvExporter;
import com.cloudops.manager.aws.forensics.export.ForensicIntegritySigner;
import com.cloudops.manager.aws.forensics.model.ForensicEvidenceItem;
import com.cloudops.manager.aws.forensics.model.ForensicExportResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ForensicDeterminismTest {

    @Test
    @DisplayName("Exporting identical items must produce identical CSV and digest")
    void shouldProduceDeterministicCsv() {
        ForensicIntegritySigner signer = new ForensicIntegritySigner();
        ForensicCsvExporter exporter = new ForensicCsvExporter(signer);

        ForensicEvidenceItem i1 = new ForensicEvidenceItem("DISCOVERY", "EC2", "i-1", "123", "us-east-1", "Discovery", Map.of("a", "b"));
        ForensicEvidenceItem i2 = new ForensicEvidenceItem("COMPLIANCE", "RULE", "SEC-001", "123", "us-east-1", "Compliance", Map.of("x", "y"));

        ForensicExportResult r1 = exporter.export(List.of(i2, i1), "123", "us-east-1");
        ForensicExportResult r2 = exporter.export(List.of(i1, i2), "123", "us-east-1");

        assertThat(r1.content()).isEqualTo(r2.content());
        assertThat(r1.sha256Digest()).isEqualTo(r2.sha256Digest());
    }
}