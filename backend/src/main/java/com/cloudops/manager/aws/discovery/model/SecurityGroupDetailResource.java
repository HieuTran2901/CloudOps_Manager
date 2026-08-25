package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SecurityGroupDetailResource(
    String securityGroupId,
    String arn,
    String securityGroupName,
    String description,
    String vpcId,
    String ownerId,
    String accountId,
    String region,
    List<SecurityGroupRuleDetail> inboundRules,
    List<SecurityGroupRuleDetail> outboundRules,
    Map<String, String> tags,
    Instant discoveredAt
) {}