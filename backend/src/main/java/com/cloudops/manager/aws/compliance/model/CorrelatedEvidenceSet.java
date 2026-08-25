package com.cloudops.manager.aws.compliance.model;

import java.util.*;

public final class CorrelatedEvidenceSet {

    private final List<CorrelatedEvidenceItem> items;

    public CorrelatedEvidenceSet(List<CorrelatedEvidenceItem> items) {
        this.items = (items != null) ? List.copyOf(items) : List.of();
    }

    public static CorrelatedEvidenceSet empty() {
        return new CorrelatedEvidenceSet(List.of());
    }

    public List<CorrelatedEvidenceItem> allItems() {
        return items;
    }

    public List<CorrelatedEvidenceItem> findByResource(String resourceType, String resourceId, EvidenceScope scope) {
        if (resourceType == null || resourceId == null || scope == null) return List.of();
        List<CorrelatedEvidenceItem> matches = new ArrayList<>();
        for (CorrelatedEvidenceItem item : items) {
            if (resourceType.equalsIgnoreCase(item.resourceType())
                    && resourceId.equalsIgnoreCase(item.resourceId())
                    && scope.matches(item.scope())) {
                matches.add(item);
            }
        }
        return List.copyOf(matches);
    }

    public List<CorrelatedEvidenceItem> findByType(String resourceType, EvidenceScope scope) {
        if (resourceType == null || scope == null) return List.of();
        List<CorrelatedEvidenceItem> matches = new ArrayList<>();
        for (CorrelatedEvidenceItem item : items) {
            if (resourceType.equalsIgnoreCase(item.resourceType()) && scope.matches(item.scope())) {
                matches.add(item);
            }
        }
        return List.copyOf(matches);
    }

    public List<CorrelatedEvidenceItem> findByScope(EvidenceScope scope) {
        if (scope == null) return List.of();
        List<CorrelatedEvidenceItem> matches = new ArrayList<>();
        for (CorrelatedEvidenceItem item : items) {
            if (scope.matches(item.scope())) {
                matches.add(item);
            }
        }
        return List.copyOf(matches);
    }

    public boolean hasEvidence(String resourceType, EvidenceScope scope) {
        return !findByType(resourceType, scope).isEmpty();
    }
}