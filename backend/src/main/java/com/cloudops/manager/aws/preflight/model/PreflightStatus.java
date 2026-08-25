package com.cloudops.manager.aws.preflight.model;

public enum PreflightStatus {
    PASS,
    BLOCKED,
    ACCESS_DENIED,
    UNAVAILABLE,
    TIMEOUT,
    INSUFFICIENT_EVIDENCE
}