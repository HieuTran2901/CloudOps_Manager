package com.cloudops.manager.aws.preflight.model;

import java.time.Instant;
import java.util.List;

public record DeploymentPreflightResult(
        PreflightStatus overallStatus,
        String accountId,
        String region,
        String callerArn,
        List<AwsCapabilityCheck> capabilityChecks,
        Instant evaluatedAt,
        String summary
) {}