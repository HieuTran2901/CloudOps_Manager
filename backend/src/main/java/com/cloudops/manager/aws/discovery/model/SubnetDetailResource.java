package com.cloudops.manager.aws.discovery.model;

import java.util.Map;

public record SubnetDetailResource(
    String subnetId,
    String arn,
    String vpcId,
    String cidrBlock,
    String ipv6CidrBlock,
    String availabilityZone,
    String availabilityZoneId,
    String state,
    Boolean mapPublicIpOnLaunch,
    Boolean assignIpv6AddressOnCreation,
    Integer availableIpAddressCount,
    Boolean defaultForAz,
    Map<String, String> tags
) {}