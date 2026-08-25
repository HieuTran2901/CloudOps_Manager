package com.cloudops.manager.aws.discovery.model;

public record S3LifecycleRule(
    String id,
    String status,
    String prefix,
    Integer expirationDays,
    Integer noncurrentDays,
    Integer abortIncompleteMultipartUploadDays
) {}