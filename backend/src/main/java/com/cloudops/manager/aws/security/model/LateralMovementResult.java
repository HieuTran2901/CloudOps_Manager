package com.cloudops.manager.aws.security.model;

import java.util.Map;

public record LateralMovementResult(
    String sourceNodeId,
    String targetNodeId,
    ReachabilityStatus status,
    SecurityPath path,
    Map<String, Object> propagationEvidence,
    String accountId,
    String region
) {
    public LateralMovementResult {
        propagationEvidence = (propagationEvidence != null) ? Map.copyOf(propagationEvidence) : Map.of();
    }
}