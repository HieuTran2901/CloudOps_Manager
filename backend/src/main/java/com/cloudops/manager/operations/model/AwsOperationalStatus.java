package com.cloudops.manager.operations.model;

import java.time.Instant;
import java.util.Map;

public record AwsOperationalStatus(
        AwsConnectivityStatus status,
        String accountId,
        String region,
        Instant lastSuccessfulSync,
        Instant lastAttemptedSync,
        Long evidenceAgeSeconds,
        String message,
        Map<String, Object> metadata
) {}