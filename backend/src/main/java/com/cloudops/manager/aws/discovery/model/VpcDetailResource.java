package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record VpcDetailResource(
    String vpcId,
    String arn,
    String accountId,
    String region,
    String state,
    String cidrBlock,
    List<String> secondaryCidrBlocks,
    List<String> ipv6CidrBlocks,
    String dhcpOptionsId,
    String instanceTenancy,
    Boolean isDefault,
    Boolean enableDnsSupport,
    Boolean enableDnsHostnames,
    Map<String, String> tags,
    Instant discoveredAt
) {}