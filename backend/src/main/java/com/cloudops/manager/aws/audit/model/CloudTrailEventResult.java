package com.cloudops.manager.aws.audit.model;

import java.time.Instant;
import java.util.List;

public record CloudTrailEventResult(
    String accountId,
    String region,
    Instant startTime,
    Instant endTime,
    int totalEvents,
    List<CloudTrailEventResource> events,
    Instant queriedAt
) {}