package com.cloudops.manager.aws.forensics.export;

import com.cloudops.manager.aws.forensics.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class ForensicJsonExporter {

    private final ObjectMapper mapper;
    private final ForensicIntegritySigner signer;

    public ForensicJsonExporter(ForensicIntegritySigner signer) {
        this.signer = signer;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public ForensicExportResult export(List<ForensicEvidenceItem> items, String accountId, String region) {
        List<ForensicEvidenceItem> sortedItems = (items != null) ? items.stream().sorted().toList() : List.of();

        Map<String, Integer> counts = new TreeMap<>();
        for (ForensicEvidenceItem item : sortedItems) {
            counts.put(item.section(), counts.getOrDefault(item.section(), 0) + 1);
        }

        String bundleId = "BUNDLE-" + accountId + "-" + region + "-" + System.currentTimeMillis();
        ForensicMetadata initialMeta = new ForensicMetadata(
                bundleId, accountId, region, Instant.now(), ForensicExportFormat.JSON, "", sortedItems.size(), counts
        );

        ForensicEvidenceBundle bundle = new ForensicEvidenceBundle(initialMeta, sortedItems);

        try {
            String rawJson = mapper.writeValueAsString(bundle);
            String digest = signer.computeSha256(rawJson);

            ForensicMetadata finalMeta = new ForensicMetadata(
                    bundleId, accountId, region, initialMeta.generatedAt(), ForensicExportFormat.JSON, digest, sortedItems.size(), counts
            );
            ForensicEvidenceBundle finalBundle = new ForensicEvidenceBundle(finalMeta, sortedItems);
            String finalJson = mapper.writeValueAsString(finalBundle);

            String filename = "forensic-evidence-" + accountId + "-" + region + ".json";
            return new ForensicExportResult(finalMeta, finalJson, digest, "application/json", filename);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize forensic bundle to JSON", e);
        }
    }
}