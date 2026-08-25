package com.cloudops.manager.aws.sts.model;

public record AssumeRoleRequest(
    String roleArn,
    String sessionName,
    String externalId,
    Integer durationSeconds
) {
    public AssumeRoleRequest {
        if (roleArn == null || roleArn.isBlank()) {
            throw new IllegalArgumentException("roleArn must not be null or blank");
        }
        if (sessionName == null || sessionName.isBlank()) {
            throw new IllegalArgumentException("sessionName must not be null or blank");
        }
        if (durationSeconds != null && (durationSeconds < 900 || durationSeconds > 43200)) {
            throw new IllegalArgumentException("durationSeconds must be between 900 (15m) and 43200 (12h)");
        }
    }

    public static AssumeRoleRequest of(String roleArn, String sessionName) {
        return new AssumeRoleRequest(roleArn, sessionName, null, null);
    }
}