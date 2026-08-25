package com.cloudops.manager.aws.security.model;

public record SecurityReachabilityResult(
    String sourceNodeId,
    String targetNodeId,
    ReachabilityStatus status,
    SecurityPath path,
    int maxDepth,
    String accountId,
    String region
) {}