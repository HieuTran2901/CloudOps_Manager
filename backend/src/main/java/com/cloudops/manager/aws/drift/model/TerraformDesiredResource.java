package com.cloudops.manager.aws.drift.model;

import java.util.Map;

public record TerraformDesiredResource(
    TerraformResourceAddress address,
    String resourceType,
    String resourceId,
    Map<String, Object> attributes,
    String accountId,
    String region
) {}