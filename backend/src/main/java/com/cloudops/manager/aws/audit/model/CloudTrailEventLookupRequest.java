package com.cloudops.manager.aws.audit.model;

import java.time.Instant;

public record CloudTrailEventLookupRequest(
    String accountId,
    String region,
    String eventName,
    String username,
    String resourceName,
    String resourceType,
    Instant startTime,
    Instant endTime,
    Integer maxResults
) {}