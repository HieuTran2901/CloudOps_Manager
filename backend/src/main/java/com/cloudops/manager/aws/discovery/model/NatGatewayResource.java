package com.cloudops.manager.aws.discovery.model;

import java.util.Map;

public record NatGatewayResource(
    String natGatewayId,
    String vpcId,
    String subnetId,
    String state,
    String connectivityType,
    String publicIp,
    String privateIp,
    String networkInterfaceId,
    Map<String, String> tags
) {}