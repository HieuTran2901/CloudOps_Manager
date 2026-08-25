package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record InventorySummary(
    String accountId,
    String region,
    int totalCount,
    Map<CloudResourceType, Integer> countByType,
    List<CloudResource> resources,
    Instant timestamp
) {}