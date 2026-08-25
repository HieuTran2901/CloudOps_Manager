package com.cloudops.manager.aws.sts.model;

import java.time.Instant;

/**
 * Encapsulates temporary in-memory credentials resulting from STS AssumeRole.
 * Protected against accidental logging, persistence, or public API serialization.
 */
public record AssumedRoleSession(
    String accessKeyId,
    String secretAccessKey,
    String sessionToken,
    Instant expiration,
    String assumedRoleUserArn
) {
    public AssumedRoleSession {
        if (accessKeyId == null || accessKeyId.isBlank()) {
            throw new IllegalArgumentException("accessKeyId must not be null or blank");
        }
        if (secretAccessKey == null || secretAccessKey.isBlank()) {
            throw new IllegalArgumentException("secretAccessKey must not be null or blank");
        }
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new IllegalArgumentException("sessionToken must not be null or blank");
        }
        if (expiration == null) {
            throw new IllegalArgumentException("expiration must not be null");
        }
    }

    @Override
    public String toString() {
        return "AssumedRoleSession[assumedRoleUserArn=" + assumedRoleUserArn 
                + ", expiration=" + expiration 
                + ", credentials=[PROTECTED]]";
    }
}