package com.cloudops.manager.aws.forensics.export;

import com.cloudops.manager.aws.forensics.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class ForensicCsvExporter {

    private final ForensicIntegritySigner signer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ForensicCsvExporter(ForensicIntegritySigner signer) {
        this.signer = signer;
    }

    public ForensicExportResult export(List<ForensicEvidenceItem> items, String accountId, String region) {
        List<ForensicEvidenceItem> sortedItems = (items != null) ? items.stream().sorted().toList() : List.of();

        StringBuilder sb = new StringBuilder();
        sb.append("section,resourceType,resourceId,accountId,region,sourceSubsystem,facts\n");

        Map<String, Integer> counts = new TreeMap<>();
        for (ForensicEvidenceItem item : sortedItems) {
            counts.put(item.section(), counts.getOrDefault(item.section(), 0) + 1);
            String factsJson = escapeCsv(convertMapToJson(item.facts()));
            sb.append(escapeCsv(item.section())).append(",")
              .append(escapeCsv(item.resourceType())).append(",")
              .append(escapeCsv(item.resourceId())).append(",")
              .append(escapeCsv(item.accountId())).append(",")
              .append(escapeCsv(item.region())).append(",")
              .append(escapeCsv(item.sourceSubsystem())).append(",")
              .append(factsJson).append("\n");
        }

        String csvContent = sb.toString();
        String digest = signer.computeSha256(csvContent);
        String bundleId = "BUNDLE-" + accountId + "-" + region + "-" + System.currentTimeMillis();

        ForensicMetadata meta = new ForensicMetadata(
                bundleId, accountId, region, Instant.now(), ForensicExportFormat.CSV, digest, sortedItems.size(), counts
        );

        String filename = "forensic-evidence-" + accountId + "-" + region + ".csv";
        return new ForensicExportResult(meta, csvContent, digest, "text/csv", filename);
    }

    private String convertMapToJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "\"\"";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}