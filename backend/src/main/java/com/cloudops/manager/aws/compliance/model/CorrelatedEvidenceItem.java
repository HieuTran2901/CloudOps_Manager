package com.cloudops.manager.aws.compliance.model;

import java.util.Map;

public record CorrelatedEvidenceItem(
    String resourceType,
    String resourceId,
    EvidenceScope scope,
    String sourceSystem,
    Map<String, Object> observedFacts
) {}