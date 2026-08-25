package com.cloudops.manager.aws.federation.model;

public enum FederationStatus {
    FEDERATED,
    INVALID_ROLE,
    ACCESS_DENIED,
    ACCOUNT_MISMATCH,
    REGION_UNAVAILABLE,
    AWS_TIMEOUT,
    AWS_THROTTLED,
    AWS_UNAVAILABLE,
    UNKNOWN
}