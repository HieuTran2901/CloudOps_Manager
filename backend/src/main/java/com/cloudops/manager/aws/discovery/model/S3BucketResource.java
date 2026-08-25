package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;
import java.util.Map;

public record S3BucketResource(
    String resourceId,
    CloudResourceType resourceType,
    String name,
    String region,
    String accountId,
    String status,
    String arn,
    Map<String, String> tags,
    Instant discoveredAt,
    Instant creationDate
) implements CloudResource {}