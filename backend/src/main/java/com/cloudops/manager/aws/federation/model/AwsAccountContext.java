package com.cloudops.manager.aws.federation.model;

public record AwsAccountContext(
        String accountId,
        String accountName,
        String defaultRegion,
        String roleArn,
        boolean isCurrent,
        FederationStatus status
) {}