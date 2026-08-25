package com.cloudops.manager.aws.audit.model;

import java.time.Instant;
import java.util.List;

public record CloudTrailEventResource(
    String eventId,
    String eventName,
    String eventSource,
    Instant eventTime,
    String awsRegion,
    CloudTrailEventIdentity userIdentity,
    String sourceIpAddress,
    String userAgent,
    List<CloudTrailEventResourceReference> resources,
    Boolean readOnly,
    String accessKeyId,
    String eventCategory
) {}