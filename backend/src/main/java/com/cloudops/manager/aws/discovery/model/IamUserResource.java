package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;

public record IamUserResource(
    String userName,
    String userId,
    String arn,
    String path,
    Instant createDate,
    String accountId
) {}