package com.cloudops.manager.aws.drift.model;

import java.util.List;

public record DriftResourceResult(
    String resourceAddress,
    String resourceType,
    String resourceId,
    DriftStatus status,
    List<DriftAttributeDifference> differences,
    String explanation
) {
    public DriftResourceResult {
        differences = (differences != null) ? List.copyOf(differences) : List.of();
    }
}