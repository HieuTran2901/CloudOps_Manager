package com.cloudops.manager.aws.discovery.model;

public record S3PublicAccessBlock(
    String status,
    Boolean blockPublicAcls,
    Boolean ignorePublicAcls,
    Boolean blockPublicPolicy,
    Boolean restrictPublicBuckets
) {}