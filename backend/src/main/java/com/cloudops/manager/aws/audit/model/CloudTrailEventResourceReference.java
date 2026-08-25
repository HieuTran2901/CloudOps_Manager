package com.cloudops.manager.aws.audit.model;

public record CloudTrailEventResourceReference(
    String resourceType,
    String resourceName
) {}