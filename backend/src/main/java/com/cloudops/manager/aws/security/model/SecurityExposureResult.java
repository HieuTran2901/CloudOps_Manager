package com.cloudops.manager.aws.security.model;

import java.util.Map;

public record SecurityExposureResult(
    String nodeId,
    String resourceType,
    String resourceId,
    ExposureStatus status,
    Map<String, Object> exposureEvidence,
    String accountId,
    String region
) {
    public SecurityExposureResult {
        exposureEvidence = (exposureEvidence != null) ? Map.copyOf(exposureEvidence) : Map.of();
    }
}