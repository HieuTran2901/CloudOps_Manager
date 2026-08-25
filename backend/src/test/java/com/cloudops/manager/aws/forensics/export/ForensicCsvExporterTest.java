package com.cloudops.manager.aws.forensics.export;

import com.cloudops.manager.aws.forensics.model.ForensicEvidenceItem;
import com.cloudops.manager.aws.forensics.model.ForensicExportResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ForensicCsvExporterTest {

    private final ForensicIntegritySigner signer = new ForensicIntegritySigner();
    private final ForensicCsvExporter exporter = new ForensicCsvExporter(signer);

    @Test
    @DisplayName("Should export forensic items as valid CSV format")
    void shouldExportCsv() {
        ForensicEvidenceItem item = new ForensicEvidenceItem(
                "DISCOVERY", "AWS::EC2::Instance", "i-123", "123", "us-east-1", "AwsResourceDiscoveryService", Map.of("status", "running")
        );

        ForensicExportResult result = exporter.export(List.of(item), "123", "us-east-1");

        assertThat(result.contentType()).isEqualTo("text/csv");
        assertThat(result.sha256Digest()).isNotBlank();
        assertThat(result.content()).startsWith("section,resourceType,resourceId,accountId,region,sourceSubsystem,facts\n");
        assertThat(result.content()).contains("\"DISCOVERY\",\"AWS::EC2::Instance\",\"i-123\"");
    }
}