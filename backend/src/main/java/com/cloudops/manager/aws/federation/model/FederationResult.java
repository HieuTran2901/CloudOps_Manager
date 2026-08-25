package com.cloudops.manager.aws.federation.model;

import java.time.Instant;

public record FederationResult(
        FederationStatus status,
        String targetAccountId,
        String assumedRoleArn,
        String assumedRoleSessionName,
        String region,
        String message,
        Instant federatedAt
) {}