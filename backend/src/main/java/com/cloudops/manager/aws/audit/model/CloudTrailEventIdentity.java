package com.cloudops.manager.aws.audit.model;

public record CloudTrailEventIdentity(
    String principalId,
    String username,
    String accountId,
    String identityType
) {}