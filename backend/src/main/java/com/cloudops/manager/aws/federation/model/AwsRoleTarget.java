package com.cloudops.manager.aws.federation.model;

import java.util.regex.Pattern;

public record AwsRoleTarget(
        String roleArn,
        String targetAccountId,
        String roleSessionName,
        String externalId,
        String region
) {
    private static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("^\\d{12}$");
    private static final Pattern ROLE_ARN_PATTERN = Pattern.compile("^arn:aws:iam::\\d{12}:role/.+$");

    public AwsRoleTarget {
        if (targetAccountId == null || !ACCOUNT_ID_PATTERN.matcher(targetAccountId.trim()).matches()) {
            throw new IllegalArgumentException("Invalid AWS target account ID: must be exactly 12 digits");
        }
        if (roleArn == null || !ROLE_ARN_PATTERN.matcher(roleArn.trim()).matches()) {
            throw new IllegalArgumentException("Invalid IAM Role ARN format: must be arn:aws:iam::<accountId>:role/<roleName>");
        }
        String expectedPrefix = ":iam::" + targetAccountId.trim() + ":role/";
        if (!roleArn.contains(expectedPrefix)) {
            throw new IllegalArgumentException("Role ARN account does not match target account ID: " + targetAccountId);
        }
    }
}