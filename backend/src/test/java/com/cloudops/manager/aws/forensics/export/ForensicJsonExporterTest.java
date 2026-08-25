package com.cloudops.manager.aws.forensics.export;

import com.cloudops.manager.aws.forensics.model.ForensicEvidenceItem;
import com.cloudops.manager.aws.forensics.model.ForensicExportResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ForensicJsonExporterTest {

    private final ForensicIntegritySigner signer = new ForensicIntegritySigner();
    private final ForensicJsonExporter exporter = new ForensicJsonExporter(signer);

    @Test
    @DisplayName("Should export forensic items as formatted JSON with SHA-256 integrity digest")
    void shouldExportJson() {
        ForensicEvidenceItem item = new ForensicEvidenceItem(
                "DISCOVERY", "AWS::EC2::Instance", "i-123", "123", "us-east-1", "AwsResourceDiscoveryService", Map.of("status", "running")
        );

        ForensicExportResult result = exporter.export(List.of(item), "123", "us-east-1");

        assertThat(result.contentType()).isEqualTo("application/json");
        assertThat(result.sha256Digest()).isNotBlank();
        assertThat(result.content()).contains("\"section\" : \"DISCOVERY\"");
        assertThat(result.content()).contains("\"resourceId\" : \"i-123\"");
    }
}