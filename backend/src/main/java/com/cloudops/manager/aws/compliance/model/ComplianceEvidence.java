package com.cloudops.manager.aws.compliance.model;

import java.util.Map;

public record ComplianceEvidence(
    String resourceType,
    String resourceId,
    Map<String, Object> observedFacts
) {}