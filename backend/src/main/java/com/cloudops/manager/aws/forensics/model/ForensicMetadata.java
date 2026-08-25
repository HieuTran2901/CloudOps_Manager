package com.cloudops.manager.aws.forensics.model;

import java.time.Instant;
import java.util.Map;

public record ForensicMetadata(
    String bundleId,
    String accountId,
    String region,
    Instant generatedAt,
    ForensicExportFormat format,
    String sha256Digest,
    int totalItemCount,
    Map<String, Integer> sectionCounts
) {
    public ForensicMetadata {
        sectionCounts = (sectionCounts != null) ? Map.copyOf(sectionCounts) : Map.of();
    }
}