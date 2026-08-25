package com.cloudops.manager.aws.preflight.model;

public record AwsCapabilityCheck(
        String capabilityName,
        String requiredAction,
        PreflightStatus status,
        String message
) {}