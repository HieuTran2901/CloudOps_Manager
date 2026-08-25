package com.cloudops.manager.aws.discovery.model;

import java.util.List;

public record SecurityGroupRuleDetail(
    String protocol,
    Integer fromPort,
    Integer toPort,
    List<String> ipv4Cidrs,
    List<String> ipv6Cidrs,
    List<String> prefixListIds,
    List<SecurityGroupReference> referencedSecurityGroups,
    String description
) {}