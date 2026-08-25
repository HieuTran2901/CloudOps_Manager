package com.cloudops.manager.aws.discovery.model;

public record S3BucketPolicySummary(
    String status,
    Boolean hasPolicy,
    String policyText,
    String reason
) {}