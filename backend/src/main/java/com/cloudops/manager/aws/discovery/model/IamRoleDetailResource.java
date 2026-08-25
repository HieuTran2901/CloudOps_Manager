package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record IamRoleDetailResource(
    String roleName,
    String roleId,
    String arn,
    String path,
    Instant createDate,
    String accountId,
    String description,
    Integer maxSessionDuration,
    List<IamTrustStatement> trustPolicyStatements,
    List<IamPolicyAttachmentInfo> attachedPolicies,
    List<String> inlinePolicyNames,
    List<IamInstanceProfileInfo> instanceProfiles,
    Map<String, String> tags,
    Instant discoveredAt
) {}