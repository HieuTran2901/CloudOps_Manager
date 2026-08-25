package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SecurityGroupResource(
    String resourceId,
    CloudResourceType resourceType,
    String name,
    String region,
    String accountId,
    String status,
    String arn,
    Map<String, String> tags,
    Instant discoveredAt,
    String description,
    String vpcId,
    List<IpPermissionRule> inboundRules,
    List<IpPermissionRule> outboundRules
) implements CloudResource {}