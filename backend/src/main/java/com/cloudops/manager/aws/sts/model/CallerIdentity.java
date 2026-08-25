package com.cloudops.manager.aws.sts.model;

public record CallerIdentity(
    String accountId,
    String arn,
    String userId
) {
    public CallerIdentity {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId must not be null or blank");
        }
        if (arn == null || arn.isBlank()) {
            throw new IllegalArgumentException("arn must not be null or blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be null or blank");
        }
    }
}