package com.cloudops.manager.aws.discovery.model;

public record NetworkAclRule(
    Integer ruleNumber,
    String protocol,
    String ruleAction,
    Boolean egress,
    String cidrBlock,
    String ipv6CidrBlock,
    Integer portRangeFrom,
    Integer portRangeTo,
    Integer icmpType,
    Integer icmpCode
) {}