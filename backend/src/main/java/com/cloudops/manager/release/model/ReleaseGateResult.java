package com.cloudops.manager.release.model;

import java.time.Instant;
import java.util.List;

public record ReleaseGateResult(
        ReleaseGateStatus overallStatus,
        boolean analyticsReady,
        boolean operationallyReady,
        boolean securityReady,
        boolean e2eReady,
        boolean determinismReady,
        boolean resilienceReady,
        boolean deploymentReady,
        boolean runtimeReady,
        boolean releaseReady,
        String version,
        String releaseTag,
        String accountId,
        String region,
        List<ReleaseGateCheck> checks,
        String sha256Digest,
        Instant evaluatedAt,
        String summary
) {}