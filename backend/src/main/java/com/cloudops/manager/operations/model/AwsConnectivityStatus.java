package com.cloudops.manager.operations.model;

public enum AwsConnectivityStatus {
    CONNECTED,
    AWS_ACCESS_DENIED,
    AWS_THROTTLED,
    AWS_TIMEOUT,
    AWS_UNAVAILABLE,
    PARTIAL_EVIDENCE,
    UNKNOWN
}