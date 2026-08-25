package com.cloudops.manager.aws.discovery.model;

public record SecurityGroupReference(
    String groupId,
    String userId,
    String vpcId,
    String vpcPeeringConnectionId,
    String description
) {}