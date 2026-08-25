package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;
import java.util.Map;

public record S3DetailResource(
    String bucketName,
    String arn,
    String accountId,
    String region,
    Instant creationDate,
    S3PublicAccessBlock publicAccessBlock,
    S3BucketPolicySummary policy,
    S3EncryptionConfiguration encryption,
    S3VersioningConfiguration versioning,
    S3CorsConfiguration cors,
    S3LifecycleConfiguration lifecycle,
    Map<String, String> tags,
    Instant discoveredAt
) {}