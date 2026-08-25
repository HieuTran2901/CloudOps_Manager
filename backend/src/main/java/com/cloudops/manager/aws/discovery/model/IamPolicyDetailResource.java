package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;
import java.util.List;

public record IamPolicyDetailResource(
    String policyArn,
    String policyName,
    String policyId,
    String path,
    Boolean isAttachable,
    Integer attachmentCount,
    String defaultVersionId,
    Instant createDate,
    Instant updateDate,
    String policyType,
    List<IamPolicyStatement> statements,
    Instant discoveredAt
) {}