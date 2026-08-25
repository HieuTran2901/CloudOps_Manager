package com.cloudops.manager.aws.forensics.model;

import java.util.Map;

public record ForensicEvidenceItem(
    String section,
    String resourceType,
    String resourceId,
    String accountId,
    String region,
    String sourceSubsystem,
    Map<String, Object> facts
) implements Comparable<ForensicEvidenceItem> {

    public ForensicEvidenceItem {
        facts = (facts != null) ? Map.copyOf(facts) : Map.of();
    }

    @Override
    public int compareTo(ForensicEvidenceItem o) {
        int c1 = this.section.compareTo(o.section);
        if (c1 != 0) return c1;
        int c2 = this.resourceType.compareTo(o.resourceType);
        if (c2 != 0) return c2;
        int c3 = this.resourceId.compareTo(o.resourceId);
        if (c3 != 0) return c3;
        int c4 = this.accountId.compareTo(o.accountId);
        if (c4 != 0) return c4;
        return this.region.compareTo(o.region);
    }
}