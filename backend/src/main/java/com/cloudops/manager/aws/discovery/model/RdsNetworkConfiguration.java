package com.cloudops.manager.aws.discovery.model;

import java.util.List;

public record RdsNetworkConfiguration(
    String vpcId,
    String dbSubnetGroupName,
    List<String> subnetIds,
    List<String> subnetAvailabilityZones,
    List<String> securityGroupIds,
    List<String> securityGroupNames,
    Boolean publiclyAccessible,
    String endpointAddress,
    Integer endpointPort
) {}