package com.cloudops.manager.operations.evidence.model;

import java.time.Instant;

public record EvidenceLifecycleRecord(
        String evidenceType,
        String accountId,
        String region,
        Instant capturedAt,
        Instant lastSuccessfulSync,
        Instant lastAttemptedSync,
        long ageSeconds,
        EvidenceFreshnessState freshnessState,
        String evidenceDigest
) {}