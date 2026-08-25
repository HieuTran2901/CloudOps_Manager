package com.cloudops.manager.aws.discovery.model;

import java.util.List;

public record Ec2NetworkInterfaceDetail(
    String networkInterfaceId,
    String subnetId,
    String vpcId,
    String primaryPrivateIp,
    List<String> privateIpAddresses,
    String publicIp,
    String macAddress,
    List<String> securityGroupIds,
    List<String> securityGroupNames,
    String status,
    String description,
    String interfaceType
) {}