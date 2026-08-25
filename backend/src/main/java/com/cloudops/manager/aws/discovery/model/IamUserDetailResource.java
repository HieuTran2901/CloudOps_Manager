package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record IamUserDetailResource(
    String userName,
    String userId,
    String arn,
    String path,
    Instant createDate,
    String accountId,
    Boolean mfaEnabled,
    List<IamMfaDeviceInfo> mfaDevices,
    List<IamAccessKeyMetadata> accessKeys,
    List<String> groupNames,
    List<IamPolicyAttachmentInfo> attachedPolicies,
    List<String> inlinePolicyNames,
    Map<String, String> tags,
    Instant discoveredAt
) {}