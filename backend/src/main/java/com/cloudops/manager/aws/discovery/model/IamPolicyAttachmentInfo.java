package com.cloudops.manager.aws.discovery.model;

public record IamPolicyAttachmentInfo(
    String policyName,
    String policyArn
) {}