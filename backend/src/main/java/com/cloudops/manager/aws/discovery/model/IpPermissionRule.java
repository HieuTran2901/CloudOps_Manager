package com.cloudops.manager.aws.discovery.model;

import java.util.List;

public record IpPermissionRule(
    String ipProtocol,
    Integer fromPort,
    Integer toPort,
    List<String> cidrIpv4Ranges,
    List<String> userSecurityGroupIds
) {}