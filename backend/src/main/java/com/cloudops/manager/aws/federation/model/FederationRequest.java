package com.cloudops.manager.aws.federation.model;

public record FederationRequest(
        String targetAccountId,
        String roleArn,
        String roleSessionName,
        String region,
        String externalId
) {}