package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;

public record IamAccessKeyMetadata(
    String accessKeyId,
    String status,
    Instant createDate,
    Instant lastUsedDate,
    String lastUsedServiceName,
    String lastUsedRegion
) {}