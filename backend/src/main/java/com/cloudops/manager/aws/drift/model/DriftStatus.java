package com.cloudops.manager.aws.drift.model;

public enum DriftStatus {
    IN_SYNC,
    DRIFTED,
    NOT_FOUND,
    UNSUPPORTED,
    INSUFFICIENT_EVIDENCE
}