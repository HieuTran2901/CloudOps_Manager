package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;
import java.util.List;

public record VpcTopologyResource(
    VpcDetailResource vpc,
    List<SubnetDetailResource> subnets,
    List<RouteTableDetailResource> routeTables,
    List<InternetGatewayResource> internetGateways,
    List<NatGatewayResource> natGateways,
    List<NetworkAclResource> networkAcls,
    List<VpcPeeringResource> peeringConnections,
    Instant discoveredAt
) {}