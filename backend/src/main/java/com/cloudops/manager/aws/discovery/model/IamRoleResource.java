package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;

public record IamRoleResource(
    String roleName,
    String roleId,
    String arn,
    String path,
    Instant createDate,
    String accountId
) {}