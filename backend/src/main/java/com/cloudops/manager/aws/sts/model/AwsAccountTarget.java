package com.cloudops.manager.aws.sts.model;

import java.util.regex.Pattern;

public record AwsAccountTarget(
    String accountId,
    String roleArn,
    String roleSessionName,
    String externalId,
    String region
) {
    private static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("^\\d{12}$");
    private static final Pattern ROLE_ARN_PATTERN = Pattern.compile("^arn:aws:iam::\\d{12}:role/.+$");

    public AwsAccountTarget {
        if (accountId == null || !ACCOUNT_ID_PATTERN.matcher(accountId.trim()).matches()) {
            throw new IllegalArgumentException("Invalid AWS Account ID: must be exactly 12 digits");
        }
        if (roleArn == null || !ROLE_ARN_PATTERN.matcher(roleArn.trim()).matches()) {
            throw new IllegalArgumentException("Invalid IAM Role ARN format: must be arn:aws:iam::<accountId>:role/<roleName>");
        }
        String expectedPrefix = ":iam::" + accountId.trim() + ":role/";
        if (!roleArn.contains(expectedPrefix)) {
            throw new IllegalArgumentException("Target role ARN does not match target account ID: " + accountId);
        }
        if (roleSessionName == null || roleSessionName.isBlank()) {
            roleSessionName = "cloudops-discovery-" + System.currentTimeMillis();
        }
    }
}