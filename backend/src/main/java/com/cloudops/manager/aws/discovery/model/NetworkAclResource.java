package com.cloudops.manager.aws.discovery.model;

import java.util.List;
import java.util.Map;

public record NetworkAclResource(
    String networkAclId,
    String vpcId,
    Boolean isDefault,
    List<String> associatedSubnetIds,
    List<NetworkAclRule> entries,
    Map<String, String> tags
) {}