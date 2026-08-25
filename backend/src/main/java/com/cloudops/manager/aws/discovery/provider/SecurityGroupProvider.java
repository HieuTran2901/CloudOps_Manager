package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.SecurityGroupDetailResource;
import com.cloudops.manager.aws.discovery.model.SecurityGroupResource;
import com.cloudops.manager.aws.discovery.model.SecurityGroupTopologyResource;

import java.util.List;
import java.util.Optional;

public interface SecurityGroupProvider {
    List<SecurityGroupResource> describeSecurityGroups(String region, String accountId);
    Optional<SecurityGroupDetailResource> getSecurityGroup(String securityGroupId, String region, String accountId);
    Optional<SecurityGroupTopologyResource> getSecurityGroupTopology(String securityGroupId, String region, String accountId);
}