package com.cloudops.manager.aws.forensics.model;

import java.util.List;

public record ForensicEvidenceBundle(
    ForensicMetadata metadata,
    List<ForensicEvidenceItem> items
) {
    public ForensicEvidenceBundle {
        items = (items != null) ? List.copyOf(items) : List.of();
    }
}