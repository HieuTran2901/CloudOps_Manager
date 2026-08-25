package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;

public record IamInstanceProfileInfo(
    String instanceProfileName,
    String instanceProfileId,
    String arn,
    String path,
    Instant createDate
) {}